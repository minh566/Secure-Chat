package com.securechat.data.remote.webrtc

import android.content.Context
import android.os.Build
import android.media.AudioManager
import android.util.Log
import com.securechat.data.remote.signaling.SignalingApiClient
import com.securechat.data.remote.signaling.TurnCredentials
import com.securechat.domain.repository.CallRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import org.webrtc.*
import org.webrtc.audio.JavaAudioDeviceModule
import org.webrtc.Camera1Enumerator
import org.webrtc.CameraVideoCapturer
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebRTCManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val callRepository: CallRepository,
    private val signalingApiClient: SignalingApiClient
) {
    private companion object {
        const val TAG = "WebRTCManager"
        const val ROLE_CALLER = "caller"
        const val ROLE_CALLEE = "callee"
    }

    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null
    private var localVideoTrack: VideoTrack? = null
    private var localAudioTrack: AudioTrack? = null
    private var videoCapturer: VideoCapturer? = null
    private var cameraEnumerator: CameraEnumerator? = null
    private var currentCameraDeviceName: String? = null
    private var surfaceHelper: SurfaceTextureHelper? = null
    private var remoteVideoTrack: VideoTrack? = null
    private val pendingRemoteIceCandidates = mutableListOf<IceCandidate>()
    private val processedRemoteCandidateKeys = mutableSetOf<String>()
    private var localIceRole: String = ROLE_CALLER
    private var remoteIceRole: String = ROLE_CALLEE
    private val _iceConnectionState = MutableStateFlow<PeerConnection.IceConnectionState?>(null)
    val iceConnectionState: StateFlow<PeerConnection.IceConnectionState?> = _iceConnectionState.asStateFlow()
    private val _peerConnectionState = MutableStateFlow<PeerConnection.PeerConnectionState?>(null)
    val peerConnectionState: StateFlow<PeerConnection.PeerConnectionState?> = _peerConnectionState.asStateFlow()
    private val audioManager: AudioManager by lazy {
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    private var eglBase: EglBase = EglBase.create()
    val eglContext: EglBase.Context
        get() = eglBase.eglBaseContext

    var localVideoSink: VideoSink? = null
        set(value) {
            field = value
            value?.let { localVideoTrack?.addSink(it) }
        }
    var remoteVideoSink: VideoSink? = null
        set(value) {
            field = value
            value?.let { sink ->
                remoteVideoTrack?.addSink(sink)
            }
        }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun clearLocalVideoSink() {
        localVideoSink = null
    }

    fun clearRemoteVideoSink() {
        remoteVideoSink = null
    }

    fun recycleEglBaseIfEligible(localReleased: Boolean, remoteReleased: Boolean) {
        if (!RendererReleasePolicy.shouldReleaseEgl(localReleased, remoteReleased)) return
        runCatching {
            eglBase.release()
            eglBase = EglBase.create()
        }
    }

    fun initialize() {
        if (peerConnectionFactory != null) return
        
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(context)
                .setEnableInternalTracer(true)
                .createInitializationOptions()
        )

        // Cấu hình Âm thanh chuyên nghiệp
        val audioDeviceModule = JavaAudioDeviceModule.builder(context)
            .setUseHardwareAcousticEchoCanceler(true)
            .setUseHardwareNoiseSuppressor(true)
            .createAudioDeviceModule()

        val options = PeerConnectionFactory.Options()
        peerConnectionFactory = PeerConnectionFactory.builder()
            .setOptions(options)
            .setAudioDeviceModule(audioDeviceModule)
            .createPeerConnectionFactory()
            
        // Thiết lập chế độ Audio cho Android
        configureInCallAudio(audioManager)
    }

    private fun configureInCallAudio(audioManager: AudioManager) {
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val speaker = audioManager.availableCommunicationDevices.firstOrNull {
                it.type == android.media.AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
            }
            if (speaker != null) {
                audioManager.setCommunicationDevice(speaker)
            }
        } else {
            @Suppress("DEPRECATION")
            run {
                audioManager.isSpeakerphoneOn = true
            }
        }
    }

    private fun resetAudioRouting(audioManager: AudioManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            audioManager.clearCommunicationDevice()
        } else {
            @Suppress("DEPRECATION")
            run {
                audioManager.isSpeakerphoneOn = false
            }
        }
        audioManager.mode = AudioManager.MODE_NORMAL
    }

    fun startLocalStream(): VideoTrack {
        localVideoTrack?.let { return it }

        val factory = peerConnectionFactory ?: throw IllegalStateException("Factory not initialized")
        val videoSource = factory.createVideoSource(false)
        surfaceHelper = SurfaceTextureHelper.create("CameraThread", eglContext)

        val capturer = createBestCameraCapturer()
            ?: throw IllegalStateException("No camera capturer available")
        videoCapturer = capturer

        capturer.initialize(surfaceHelper, context, videoSource.capturerObserver)
        try {
            capturer.startCapture(1280, 720, 30)
            Log.i(TAG, "startCapture success: 1280x720@30")
        } catch (e: Exception) {
            Log.e(TAG, "startCapture failed", e)
            throw e
        }

        localVideoTrack = factory.createVideoTrack("local_video", videoSource)
        localVideoTrack?.setEnabled(true)
        localVideoSink?.let { localVideoTrack?.addSink(it) }

        val audioSource = factory.createAudioSource(MediaConstraints())
        localAudioTrack = factory.createAudioTrack("local_audio", audioSource)
        localAudioTrack?.setEnabled(true)

        return localVideoTrack!!
    }

    private fun createBestCameraCapturer(): VideoCapturer? {
        val camera2Enumerator = Camera2Enumerator(context)
        val camera2Front = camera2Enumerator.deviceNames.firstOrNull { camera2Enumerator.isFrontFacing(it) }
        val camera2Name = camera2Front ?: camera2Enumerator.deviceNames.firstOrNull()
        if (camera2Name != null) {
            val camera2Capturer = camera2Enumerator.createCapturer(camera2Name, null)
            if (camera2Capturer != null) {
                Log.i(TAG, "Using Camera2 capturer: $camera2Name")
                cameraEnumerator = camera2Enumerator
                currentCameraDeviceName = camera2Name
                return camera2Capturer
            }
        }

        val camera1Enumerator = Camera1Enumerator(false)
        val camera1Front = camera1Enumerator.deviceNames.firstOrNull { camera1Enumerator.isFrontFacing(it) }
        val camera1Name = camera1Front ?: camera1Enumerator.deviceNames.firstOrNull()
        if (camera1Name != null) {
            val camera1Capturer = camera1Enumerator.createCapturer(camera1Name, null)
            if (camera1Capturer != null) {
                Log.i(TAG, "Using Camera1 capturer fallback: $camera1Name")
                cameraEnumerator = camera1Enumerator
                currentCameraDeviceName = camera1Name
                return camera1Capturer
            }
        }

        Log.e(TAG, "Failed to create any camera capturer")
        return null
    }

    fun setAudioEnabled(enabled: Boolean) { localAudioTrack?.setEnabled(enabled) }
    fun setVideoEnabled(enabled: Boolean) { localVideoTrack?.setEnabled(enabled) }

    fun setSpeakerEnabled(enabled: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (enabled) {
                val speaker = audioManager.availableCommunicationDevices.firstOrNull {
                    it.type == android.media.AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
                }
                if (speaker != null) {
                    audioManager.setCommunicationDevice(speaker)
                }
            } else {
                audioManager.clearCommunicationDevice()
            }
        } else {
            @Suppress("DEPRECATION")
            run {
                audioManager.isSpeakerphoneOn = enabled
            }
        }
    }

    fun switchCamera() {
        val capturer = videoCapturer as? CameraVideoCapturer ?: return
        val enumerator = cameraEnumerator ?: return
        val current = currentCameraDeviceName ?: return
        val target = enumerator.deviceNames.firstOrNull { it != current } ?: return

        capturer.switchCamera(object : CameraVideoCapturer.CameraSwitchHandler {
            override fun onCameraSwitchDone(isFrontCamera: Boolean) {
                currentCameraDeviceName = target
                Log.i(TAG, "Camera switched to $target, front=$isFrontCamera")
            }

            override fun onCameraSwitchError(errorDescription: String?) {
                Log.e(TAG, "switchCamera failed: $errorDescription")
            }
        }, target)
    }

    private suspend fun createPeerConnection(sessionId: String): PeerConnection {
        val turnCredentials = runCatching { signalingApiClient.fetchTurnCredentials() }
            .onFailure { Log.w(TAG, "Failed to fetch TURN credentials, using STUN-only", it) }
            .getOrNull()
        if (turnCredentials == null) {
            Log.w(TAG, "TURN credentials unavailable, continuing with STUN-only ICE servers")
        }

        val iceServers = buildIceServers(turnCredentials)
        val config = PeerConnection.RTCConfiguration(iceServers).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
        }

        val observer = object : PeerConnection.Observer {
            override fun onIceCandidate(candidate: IceCandidate) {
                scope.launch {
                    val mid = candidate.sdpMid ?: ""
                    val payload = "$mid|${candidate.sdpMLineIndex}|${candidate.sdp}"
                    Log.d(TAG, "onIceCandidate[$localIceRole] mid=$mid, index=${candidate.sdpMLineIndex}")
                    callRepository.sendIceCandidate(sessionId, localIceRole, payload)
                }
            }

            override fun onTrack(transceiver: RtpTransceiver) {
                attachRemoteTrack(transceiver.receiver.track())
            }

            override fun onIceConnectionChange(state: PeerConnection.IceConnectionState) {
                Log.i(TAG, "onIceConnectionChange: $state (role=$localIceRole)")
                _iceConnectionState.value = state
            }
            override fun onIceConnectionReceivingChange(p0: Boolean) {}
            override fun onSignalingChange(p0: PeerConnection.SignalingState?) {}
            override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState?) {}
            override fun onAddStream(p0: MediaStream?) {}
            override fun onRemoveStream(p0: MediaStream?) {}
            override fun onDataChannel(p0: DataChannel?) {}
            override fun onRenegotiationNeeded() {}
            override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>?) {}
            override fun onConnectionChange(p0: PeerConnection.PeerConnectionState?) {
                Log.i(TAG, "onConnectionChange: $p0 (role=$localIceRole)")
                _peerConnectionState.value = p0
            }
            override fun onSelectedCandidatePairChanged(p0: CandidatePairChangeEvent?) {}
            override fun onAddTrack(p0: RtpReceiver?, p1: Array<out MediaStream>?) {
                attachRemoteTrack(p0?.track())
            }
        }

        val pc = peerConnectionFactory!!.createPeerConnection(config, observer)!!
        
        // ═══ Cấu hình Transceiver: SEND_RECV (rõ ràng) ═══
        val sendRecvInit = RtpTransceiver.RtpTransceiverInit(RtpTransceiver.RtpTransceiverDirection.SEND_RECV)
        
        if (localVideoTrack != null) {
            pc.addTransceiver(localVideoTrack, sendRecvInit)
            Log.i(TAG, "addTransceiver[video-track] SEND_RECV")
        } else {
            pc.addTransceiver(MediaStreamTrack.MediaType.MEDIA_TYPE_VIDEO, sendRecvInit)
            Log.w(TAG, "addTransceiver[video-media] SEND_RECV (no local video track yet)")
        }

        if (localAudioTrack != null) {
            pc.addTransceiver(localAudioTrack, sendRecvInit)
            Log.i(TAG, "addTransceiver[audio-track] SEND_RECV")
        } else {
            pc.addTransceiver(MediaStreamTrack.MediaType.MEDIA_TYPE_AUDIO, sendRecvInit)
            Log.w(TAG, "addTransceiver[audio-media] SEND_RECV (no local audio track yet)")
        }
        enforceSendRecvTransceivers(pc)
        return pc
    }

    private fun enforceSendRecvTransceivers(pc: PeerConnection) {
        pc.transceivers.forEachIndexed { index, transceiver ->
            val before = transceiver.direction
            runCatching {
                transceiver.direction = RtpTransceiver.RtpTransceiverDirection.SEND_RECV
            }
            Log.i(
                TAG,
                "transceiver[$index] mid=${transceiver.mid} media=${transceiver.mediaType} direction=$before -> ${transceiver.direction}"
            )
        }
    }

    private fun buildIceServers(turnCredentials: TurnCredentials?): List<PeerConnection.IceServer> {
        val specs = TurnIceServerPolicy.resolve(turnCredentials)
        return specs.map { spec ->
            val builder = PeerConnection.IceServer.builder(spec.url)
            if (!spec.username.isNullOrBlank()) builder.setUsername(spec.username)
            if (!spec.credential.isNullOrBlank()) builder.setPassword(spec.credential)
            builder.createIceServer()
        }
    }

    fun call(sessionId: String) {
        localIceRole = ROLE_CALLER
        remoteIceRole = ROLE_CALLEE
        pendingRemoteIceCandidates.clear()
        processedRemoteCandidateKeys.clear()
        
        Log.i(TAG, "╔═══ CALL INITIATED ═══")
        Log.i(TAG, "║ localRole: $localIceRole")
        Log.i(TAG, "║ remoteRole: $remoteIceRole")
        Log.i(TAG, "╚════════════════════════")

        scope.launch {
            peerConnection = createPeerConnection(sessionId)
            val constraints = MediaConstraints().apply {
                mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
                mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            }
            peerConnection?.createOffer(object : SdpObserverAdapter() {
                override fun onCreateSuccess(sdp: SessionDescription?) {
                    sdp?.let {
                        logSdpMediaSections("local-offer", it.description)
                        peerConnection?.setLocalDescription(object : SdpObserverAdapter() {
                            override fun onSetSuccess() {
                                scope.launch {
                                    Log.d(TAG, "Sending OFFER from $localIceRole")
                                    callRepository.sendOffer(sessionId, it.description)
                                }
                            }

                            override fun onSetFailure(error: String?) {
                                Log.e(TAG, "setLocalDescription(offer) failed: $error")
                            }
                        }, it)
                    }
                }

                override fun onCreateFailure(error: String?) {
                    Log.e(TAG, "createOffer failed: $error")
                }
            }, constraints)

            callRepository.observeAnswer(sessionId).collectLatest { answerSdp ->
                answerSdp?.let {
                    logSdpMediaSections("remote-answer", it)
                    peerConnection?.setRemoteDescription(object : SdpObserverAdapter() {
                        override fun onSetSuccess() {
                            Log.d(TAG, "Remote description (ANSWER) set, flushing ICE candidates...")
                            flushPendingIceCandidates()
                        }

                        override fun onSetFailure(error: String?) {
                            Log.e(TAG, "setRemoteDescription(answer) failed: $error")
                        }
                    }, SessionDescription(SessionDescription.Type.ANSWER, it))
                }
            }
        }
        observeRemoteIceCandidates(sessionId)
    }

    fun answer(sessionId: String, offerSdp: String) {
        localIceRole = ROLE_CALLEE
        remoteIceRole = ROLE_CALLER
        pendingRemoteIceCandidates.clear()
        processedRemoteCandidateKeys.clear()

        Log.i(TAG, "╔═══ CALL ANSWERED ═══")
        Log.i(TAG, "║ localRole: $localIceRole")
        Log.i(TAG, "║ remoteRole: $remoteIceRole")
        Log.i(TAG, "╚════════════════════════")

        scope.launch {
            peerConnection = createPeerConnection(sessionId)
            logSdpMediaSections("remote-offer", offerSdp)
            peerConnection?.setRemoteDescription(object : SdpObserverAdapter() {
                override fun onSetSuccess() {
                    Log.d(TAG, "Remote description (OFFER) set, flushing ICE candidates...")
                    flushPendingIceCandidates()
                    createAndSendAnswer(sessionId)
                }

                override fun onSetFailure(error: String?) {
                    Log.e(TAG, "setRemoteDescription(offer) failed: $error")
                }
            }, SessionDescription(SessionDescription.Type.OFFER, offerSdp))
        }
        observeRemoteIceCandidates(sessionId)
    }

    private fun createAndSendAnswer(sessionId: String) {
        peerConnection?.createAnswer(object : SdpObserverAdapter() {
            override fun onCreateSuccess(sdp: SessionDescription?) {
                sdp?.let {
                    logSdpMediaSections("local-answer", it.description)
                    peerConnection?.setLocalDescription(object : SdpObserverAdapter() {
                        override fun onSetSuccess() {
                            scope.launch {
                                Log.d(TAG, "Sending ANSWER from $localIceRole")
                                callRepository.sendAnswer(sessionId, it.description)
                            }
                        }

                        override fun onSetFailure(error: String?) {
                            Log.e(TAG, "setLocalDescription(answer) failed: $error")
                        }
                    }, it)
                }
            }

            override fun onCreateFailure(error: String?) {
                Log.e(TAG, "createAnswer failed: $error")
            }
        }, MediaConstraints())
    }

    private fun observeRemoteIceCandidates(sessionId: String) {
        scope.launch {
            callRepository.observeIceCandidates(sessionId, remoteIceRole).collect { candidates ->
                Log.d(TAG, "observeIceCandidates[$remoteIceRole] received ${candidates.size} candidate(s)")
                
                candidates.forEach { payload ->
                    if (!processedRemoteCandidateKeys.add(payload)) {
                        Log.d(TAG, "  ↳ [DUPLICATE] skipped already processed candidate")
                        return@forEach
                    }

                    val firstSep = payload.indexOf('|')
                    val secondSep = if (firstSep >= 0) payload.indexOf('|', firstSep + 1) else -1
                    if (firstSep <= 0 || secondSep <= firstSep) {
                        Log.w(TAG, "  ✗ Invalid ICE[$remoteIceRole] candidate format: $payload")
                        return@forEach
                    }

                    val midRaw = payload.substring(0, firstSep)
                    val mLineIndex = payload.substring(firstSep + 1, secondSep).toIntOrNull()
                    val candidateSdp = payload.substring(secondSep + 1)
                    if (mLineIndex != null && candidateSdp.isNotBlank()) {
                        val sdpMid = midRaw.takeIf { it.isNotBlank() && it != "null" }
                        val candidate = IceCandidate(sdpMid, mLineIndex, candidateSdp)
                        val pc = peerConnection
                        
                        // Log candidate details
                        Log.d(TAG, "  ✦ ICE[$remoteIceRole] mid='$sdpMid' index=$mLineIndex sdp=${candidateSdp.take(40)}...")
                        
                        if (pc?.remoteDescription == null) {
                            pendingRemoteIceCandidates.add(candidate)
                            Log.d(TAG, "    → QUEUED (remoteDescription not ready)")
                        } else {
                            val added = pc.addIceCandidate(candidate)
                            if (added) {
                                Log.d(TAG, "    → ADDED to PeerConnection")
                            } else {
                                Log.w(TAG, "    → FAILED, queueing for retry")
                                pendingRemoteIceCandidates.add(candidate)
                            }
                        }
                    } else {
                        Log.w(TAG, "  ✗ Invalid ICE[$remoteIceRole] candidate format: $payload")
                    }
                }
            }
        }
    }

    private fun logSdpMediaSections(label: String, sdp: String) {
        val sections = mutableListOf<Pair<String, String>>()
        var currentType: String? = null
        var currentDir = "unspecified"

        sdp.lineSequence().forEach { rawLine ->
            val line = rawLine.trim()
            if (line.startsWith("m=")) {
                currentType?.let { sections.add(it to currentDir) }
                currentType = line.removePrefix("m=").substringBefore(' ')
                currentDir = "unspecified"
            } else if (line == "a=sendrecv") {
                currentDir = "sendrecv"
            } else if (line == "a=sendonly") {
                currentDir = "sendonly"
            } else if (line == "a=recvonly") {
                currentDir = "recvonly"
            } else if (line == "a=inactive") {
                currentDir = "inactive"
            }
        }
        currentType?.let { sections.add(it to currentDir) }

        if (sections.isEmpty()) {
            Log.w(TAG, "SDP[$label] has no media sections")
            return
        }

        val summary = sections.joinToString(" | ") { "${it.first}:${it.second}" }
        Log.i(TAG, "╔═══ SDP[$label] Media Sections ═══")
        Log.i(TAG, "║ $summary")
        
        // Log each media section detail
        sections.forEachIndexed { index, (type, direction) ->
            Log.i(TAG, "║ [$index] m=$type → a=$direction")
        }

        val videoDirection = sections.firstOrNull { it.first == "video" }?.second
        when (videoDirection) {
            null -> Log.w(TAG, "║ ⚠️  missing m=video section")
            "sendrecv" -> Log.i(TAG, "║ ✓ m=video is BIDIRECTIONAL (sendrecv)")
            else -> Log.w(TAG, "║ ⚠️  m=video direction is '$videoDirection' (not bidirectional)")
        }
        
        Log.i(TAG, "╚═══════════════════════════════════")
    }

    private fun attachRemoteTrack(track: MediaStreamTrack?) {
        if (track is VideoTrack) {
            Log.i(TAG, "Remote VideoTrack attached")
            remoteVideoTrack = track
            remoteVideoSink?.let { sink ->
                track.addSink(sink)
            }
        } else {
            Log.d(TAG, "attachRemoteTrack ignored non-video track=${track?.kind()}")
        }
    }

    private fun flushPendingIceCandidates() {
        val pc = peerConnection ?: return
        if (pc.remoteDescription == null || pendingRemoteIceCandidates.isEmpty()) return

        Log.i(TAG, "╔═══ Flushing ${pendingRemoteIceCandidates.size} ICE Candidates ═══")
        var successCount = 0
        var failCount = 0
        
        pendingRemoteIceCandidates.forEach { candidate ->
            val added = pc.addIceCandidate(candidate)
            if (added) {
                successCount++
                Log.d(TAG, "  ✓ Added ICE candidate from $remoteIceRole")
            } else {
                failCount++
                Log.w(TAG, "  ✗ Failed to add ICE candidate from $remoteIceRole")
            }
        }
        
        Log.i(TAG, "║ Result: $successCount succeeded, $failCount failed")
        Log.i(TAG, "╚═══════════════════════════════════════════════════════════")
        pendingRemoteIceCandidates.clear()
    }

    fun endCall() {
        try {
            videoCapturer?.stopCapture()
            videoCapturer?.dispose()
            surfaceHelper?.dispose()
            peerConnection?.close()
            localVideoTrack?.dispose()
            localAudioTrack?.dispose()
            peerConnection = null
            localVideoTrack = null
            localAudioTrack = null
            localVideoSink = null
            remoteVideoSink = null
            remoteVideoTrack = null
            pendingRemoteIceCandidates.clear()
            processedRemoteCandidateKeys.clear()
            _iceConnectionState.value = null
            _peerConnectionState.value = null
            scope.coroutineContext.cancelChildren()
            runCatching {
                eglBase.release()
                eglBase = EglBase.create()
            }
            
            // Trả lại âm thanh bình thường
            resetAudioRouting(audioManager)
        } catch (e: Exception) {}
    }
}

internal data class IceServerSpec(
    val url: String,
    val username: String? = null,
    val credential: String? = null
)

internal object TurnIceServerPolicy {
    private val defaultStunSpecs = listOf(
        IceServerSpec("stun:stun.l.google.com:19302"),
        IceServerSpec("stun:stun1.l.google.com:19302"),
        IceServerSpec("stun:stun2.l.google.com:19302")
    )

    fun resolve(turnCredentials: TurnCredentials?): List<IceServerSpec> {
        if (turnCredentials == null) return defaultStunSpecs

        val turnSpecs = turnCredentials.urls
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .map {
                IceServerSpec(
                    url = it,
                    username = turnCredentials.username,
                    credential = turnCredentials.credential
                )
            }

        return if (turnSpecs.isEmpty()) defaultStunSpecs else defaultStunSpecs + turnSpecs
    }
}

internal object RendererReleasePolicy {
    fun shouldReleaseEgl(localReleased: Boolean, remoteReleased: Boolean): Boolean {
        return localReleased && remoteReleased
    }
}

open class SdpObserverAdapter : SdpObserver {
    override fun onCreateSuccess(sdp: SessionDescription?) {}
    override fun onSetSuccess() {}
    override fun onCreateFailure(error: String?) {}
    override fun onSetFailure(error: String?) {}
}
