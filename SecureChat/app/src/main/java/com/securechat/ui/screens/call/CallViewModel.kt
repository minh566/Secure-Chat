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
import org.webrtc.SurfaceViewRenderer
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
    private val calleeId: String = savedStateHandle["calleeId"] ?: ""

    private val endHandled = AtomicBoolean(false)
    private var disconnectTimeoutJob: Job? = null

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
    }

    private fun setupCall() {
        viewModelScope.launch {
            if (isCaller) {
                val currentUser = authRepository.currentUser ?: return@launch
                val session = CallSession(
                    id = sessionId,
                    callerId = currentUser.uid,
                    calleeId = calleeId,
                    status = CallStatus.RINGING
                )
                callRepository.initiateCall(session)
                
                webRTCManager.startLocalStream()
                webRTCManager.call(sessionId)
            } else {
                callRepository.acceptCall(sessionId)
                webRTCManager.startLocalStream()
                callRepository.observeOffer(sessionId).filterNotNull().first().let { offerSdp ->
                    webRTCManager.answer(sessionId, offerSdp)
                }
            }
            _uiState.update { it.copy(status = CallStatus.ACCEPTED) }
        }
    }

    fun getEglContext() = webRTCManager.eglContext
    fun setLocalSink(renderer: SurfaceViewRenderer) { webRTCManager.localVideoSink = renderer }
    fun setRemoteSink(renderer: SurfaceViewRenderer) { webRTCManager.remoteVideoSink = renderer }

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
