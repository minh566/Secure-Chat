package com.securechat.ui.screens.call

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.securechat.data.remote.webrtc.WebRTCManager
import com.securechat.domain.model.CallSession
import com.securechat.domain.model.CallStatus
import com.securechat.domain.repository.AuthRepository
import com.securechat.domain.repository.CallRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.webrtc.SurfaceViewRenderer
import java.util.Date
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

data class CallUiState(
    val status: CallStatus = CallStatus.RINGING,
    val isMicMuted: Boolean = false,
    val isCameraOff: Boolean = false,
    val isSpeakerOn: Boolean = true,
    val callDurationSeconds: Int = 0,
    val errorMessage: String? = null
)

@HiltViewModel
class CallViewModel @Inject constructor(
    private val webRTCManager: WebRTCManager,
    private val callRepository: CallRepository,
    private val authRepository: AuthRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val sessionId: String = checkNotNull(savedStateHandle["sessionId"])
    private val isCaller: Boolean = savedStateHandle["isCaller"] ?: false
    private val peerId: String = savedStateHandle["peerId"] ?: ""

    private val endHandled = AtomicBoolean(false)
    private var disconnectTimeoutJob: Job? = null
    private var localRendererReleased: Boolean = false
    private var remoteRendererReleased: Boolean = false

    private val _uiState = MutableStateFlow(CallUiState())
    val uiState: StateFlow<CallUiState> = _uiState.asStateFlow()

    init {
        webRTCManager.initialize()
        observeCallStatus()
        observeWebRtcConnection()
        setupCall()
        startCallTimer()
    }

    private fun observeWebRtcConnection() {
        viewModelScope.launch {
            webRTCManager.iceConnectionState.collect { state ->
                when (state) {
                    org.webrtc.PeerConnection.IceConnectionState.CONNECTED,
                    org.webrtc.PeerConnection.IceConnectionState.COMPLETED -> {
                        disconnectTimeoutJob?.cancel()
                        disconnectTimeoutJob = null
                    }

                    org.webrtc.PeerConnection.IceConnectionState.DISCONNECTED -> {
                        if (_uiState.value.status == CallStatus.ACCEPTED && !endHandled.get()) {
                            if (disconnectTimeoutJob?.isActive != true) {
                                disconnectTimeoutJob = viewModelScope.launch {
                                    // Avoid false positives on short network hiccups.
                                    delay(3000)
                                    if (!endHandled.get()) {
                                        handleRemoteEnded(CallStatus.ENDED)
                                    }
                                }
                            }
                        }
                    }

                    org.webrtc.PeerConnection.IceConnectionState.FAILED,
                    org.webrtc.PeerConnection.IceConnectionState.CLOSED -> {
                        disconnectTimeoutJob?.cancel()
                        disconnectTimeoutJob = null
                        if (_uiState.value.status == CallStatus.ACCEPTED) {
                            handleRemoteEnded(CallStatus.ENDED)
                        }
                    }

                    else -> Unit
                }
            }
        }
    }

    private fun observeCallStatus() {
        viewModelScope.launch {
            callRepository.observeCallStatus(sessionId)
                .collect { status ->
                    // If call doc/status disappears (remote app killed or call document removed),
                    // treat it as a terminal state so local UI does not hang on call screen.
                    if (status == null) {
                        handleRemoteEnded(CallStatus.ENDED)
                        return@collect
                    }
                    when (status) {
                        CallStatus.ENDED,
                        CallStatus.DECLINED,
                        CallStatus.MISSED -> handleRemoteEnded(status)
                        CallStatus.ACCEPTED -> _uiState.update { it.copy(status = CallStatus.ACCEPTED) }
                        CallStatus.RINGING -> Unit
                    }
                }
        }
    }

    private fun handleRemoteEnded(status: CallStatus) {
        if (!endHandled.compareAndSet(false, true)) return
        webRTCManager.endCall()
        _uiState.update { it.copy(status = status) }
        viewModelScope.launch {
            try { callRepository.endCall(sessionId) } catch (e: Exception) {}
        }
    }

    private fun setupCall() {
        viewModelScope.launch {
            if (isCaller) {
                val currentUser = authRepository.currentUser ?: return@launch
                if (peerId.isBlank()) {
                    _uiState.update { it.copy(errorMessage = "Thiếu thông tin người nhận cuộc gọi", status = CallStatus.ENDED) }
                    return@launch
                }

                val session = CallSession(
                    id = sessionId,
                    callerId = currentUser.uid,
                    calleeId = peerId,
                    startedAt = Date(),
                    status = CallStatus.RINGING
                )
                callRepository.initiateCall(session)
                
                webRTCManager.startLocalStream()
                webRTCManager.call(sessionId)

                // Auto-timeout sau 3 giây nếu chưa có ACCEPTED.
                delay(3000)
                if (_uiState.value.status == CallStatus.RINGING) {
                    handleRemoteEnded(CallStatus.MISSED)
                }
            } else {
                webRTCManager.startLocalStream()
                val offerSdp = withTimeoutOrNull(3000) {
                    callRepository.observeOffer(sessionId).filterNotNull().first()
                }

                if (offerSdp == null) {
                    handleRemoteEnded(CallStatus.MISSED)
                    return@launch
                }

                offerSdp.let {
                    webRTCManager.answer(sessionId, offerSdp)
                }
                callRepository.acceptCall(sessionId)
                _uiState.update { it.copy(status = CallStatus.ACCEPTED) }
            }
        }
    }

    fun getEglContext() = webRTCManager.eglContext
    fun setLocalSink(renderer: SurfaceViewRenderer) {
        localRendererReleased = false
        webRTCManager.localVideoSink = renderer
    }

    fun setRemoteSink(renderer: SurfaceViewRenderer) {
        remoteRendererReleased = false
        webRTCManager.remoteVideoSink = renderer
    }

    fun clearLocalSink() {
        webRTCManager.clearLocalVideoSink()
    }

    fun clearRemoteSink() {
        webRTCManager.clearRemoteVideoSink()
    }

    fun onLocalRendererReleased() {
        localRendererReleased = true
        webRTCManager.recycleEglBaseIfEligible(localRendererReleased, remoteRendererReleased)
    }

    fun onRemoteRendererReleased() {
        remoteRendererReleased = true
        webRTCManager.recycleEglBaseIfEligible(localRendererReleased, remoteRendererReleased)
    }

    private fun startCallTimer() {
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(1000)
                if (_uiState.value.status == CallStatus.ACCEPTED) {
                    _uiState.update { it.copy(callDurationSeconds = it.callDurationSeconds + 1) }
                }
            }
        }
    }

    fun toggleMic() {
        val muted = !_uiState.value.isMicMuted
        _uiState.update { it.copy(isMicMuted = muted) }
        webRTCManager.setAudioEnabled(!muted)
    }

    fun toggleCamera() {
        val off = !_uiState.value.isCameraOff
        _uiState.update { it.copy(isCameraOff = off) }
        webRTCManager.setVideoEnabled(!off)
    }

    fun toggleSpeaker() {
        val speakerOn = !_uiState.value.isSpeakerOn
        _uiState.update { it.copy(isSpeakerOn = speakerOn) }
        webRTCManager.setSpeakerEnabled(speakerOn)
    }

    fun switchCamera() {
        webRTCManager.switchCamera()
    }

    fun endCall() {
        if (!endHandled.compareAndSet(false, true)) return
        disconnectTimeoutJob?.cancel()
        disconnectTimeoutJob = null

        viewModelScope.launch {
            webRTCManager.endCall()
            callRepository.endCall(sessionId)
            _uiState.update { it.copy(status = CallStatus.ENDED) }
        }
    }

    override fun onCleared() {
        super.onCleared()
        disconnectTimeoutJob?.cancel()
        disconnectTimeoutJob = null
        if (endHandled.compareAndSet(false, true)) {
            webRTCManager.endCall()
        }
    }
}
