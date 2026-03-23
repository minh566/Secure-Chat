package com.securechat.ui.screens.call

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.securechat.data.remote.webrtc.WebRTCManager
import com.securechat.domain.model.CallStatus
import com.securechat.domain.repository.CallRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
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
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val sessionId: String = checkNotNull(savedStateHandle["sessionId"])
    private val isCaller: Boolean = savedStateHandle["isCaller"] ?: false

    private val _uiState = MutableStateFlow(CallUiState())
    val uiState: StateFlow<CallUiState> = _uiState.asStateFlow()

    init {
        webRTCManager.initialize()
        setupCall()
        startCallTimer()
    }

    private fun setupCall() {
        viewModelScope.launch {
            if (isCaller) {
                webRTCManager.startLocalStream()
                webRTCManager.call(sessionId)
            } else {
                webRTCManager.startLocalStream()
                callRepository.observeOffer(sessionId).filterNotNull().first().let { offerSdp ->
                    webRTCManager.answer(sessionId, offerSdp)
                }
            }
            _uiState.update { it.copy(status = CallStatus.ACCEPTED) }
        }
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
    }

    fun toggleCamera() {
        val off = !_uiState.value.isCameraOff
        _uiState.update { it.copy(isCameraOff = off) }
    }

    fun endCall() {
        viewModelScope.launch {
            webRTCManager.endCall()
            callRepository.endCall(sessionId)
            _uiState.update { it.copy(status = CallStatus.ENDED) }
        }
    }

    override fun onCleared() {
        super.onCleared()
        webRTCManager.endCall()
    }
}
