package com.securechat.data.remote.webrtc

import android.content.Context
import com.securechat.domain.repository.CallRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import org.webrtc.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * WebRTCManager — quản lý toàn bộ vòng đời WebRTC
 *
 * Luồng hoạt động:
 * Caller: createOffer() → gửi SDP offer lên Firestore (qua CallRepository)
 *         → lắng nghe SDP answer → setRemoteDescription
 *         → gửi ICE candidates
 *
 * Callee: lắng nghe SDP offer → createAnswer() → gửi lên Firestore
 *         → setRemoteDescription
 *         → gửi ICE candidates
 */
@Singleton
class WebRTCManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val callRepository: CallRepository
) {
    // ── PeerConnection factory ────────────────────────────────────────────────
    private lateinit var peerConnectionFactory: PeerConnectionFactory
    private var peerConnection: PeerConnection? = null
    private var localVideoTrack: VideoTrack? = null
    private var localAudioTrack: AudioTrack? = null

    // Renderer surface (set từ UI)
    var localVideoSink: VideoSink? = null
    var remoteVideoSink: VideoSink? = null

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // ── Khởi tạo ─────────────────────────────────────────────────────────────
    fun initialize() {
        // Init PeerConnectionFactory (bắt buộc gọi trước tiên)
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(context)
                .setEnableInternalTracer(true)
                .createInitializationOptions()
        )
        val options = PeerConnectionFactory.Options()
        peerConnectionFactory = PeerConnectionFactory.builder()
            .setOptions(options)
            .createPeerConnectionFactory()
    }

    // ── Tạo local stream (camera + mic) ──────────────────────────────────────
    fun startLocalStream(): VideoTrack {
        val videoSource = peerConnectionFactory.createVideoSource(false)
        val surfaceHelper = SurfaceTextureHelper.create("CameraThread", EglBase.create().eglBaseContext)

        // Camera2 enumerator
        val enumerator = Camera2Enumerator(context)
        val frontCamera = enumerator.deviceNames.find { enumerator.isFrontFacing(it) }
            ?: enumerator.deviceNames.first()

        val videoCapturer = enumerator.createCapturer(frontCamera, null) as VideoCapturer
        videoCapturer.initialize(surfaceHelper, context, videoSource.capturerObserver)
        videoCapturer.startCapture(1280, 720, 30)

        localVideoTrack = peerConnectionFactory.createVideoTrack("local_video", videoSource)
        localVideoTrack!!.addSink(localVideoSink ?: return localVideoTrack!!)

        val audioSource = peerConnectionFactory.createAudioSource(MediaConstraints())
        localAudioTrack = peerConnectionFactory.createAudioTrack("local_audio", audioSource)

        return localVideoTrack!!
    }

    // ── Tạo PeerConnection ────────────────────────────────────────────────────
    private fun createPeerConnection(sessionId: String): PeerConnection {
        // STUN server — giúp tìm địa chỉ IP public (NAT traversal)
        val iceServers = listOf(
            PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
            PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer()
            // Thêm TURN server ở đây nếu cần (trả phí)
        )
        val config = PeerConnection.RTCConfiguration(iceServers).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
        }

        val observer = object : PeerConnection.Observer {
            override fun onIceCandidate(candidate: IceCandidate) {
                // Gửi ICE candidate lên Firestore để peer kia nhận
                scope.launch {
                    callRepository.sendIceCandidate(
                        sessionId,
                        "${candidate.sdpMid}|${candidate.sdpMLineIndex}|${candidate.sdp}"
                    )
                }
            }

            override fun onTrack(transceiver: RtpTransceiver) {
                // Nhận remote video stream
                val track = transceiver.receiver.track()
                if (track is VideoTrack) {
                    track.addSink(remoteVideoSink ?: return)
                }
            }

            override fun onIceConnectionChange(state: PeerConnection.IceConnectionState) {}
            override fun onIceConnectionReceivingChange(receiving: Boolean) {}
            override fun onSignalingChange(state: PeerConnection.SignalingState) {}
            override fun onIceGatheringChange(state: PeerConnection.IceGatheringState) {}
            override fun onAddStream(stream: MediaStream) {}
            override fun onRemoveStream(stream: MediaStream) {}
            override fun onDataChannel(channel: DataChannel) {}
            override fun onRenegotiationNeeded() {}
            override fun onIceCandidatesRemoved(candidates: Array<IceCandidate>) {}
            override fun onConnectionChange(state: PeerConnection.PeerConnectionState) {}
            override fun onSelectedCandidatePairChanged(event: CandidatePairChangeEvent) {}
            override fun onAddTrack(receiver: RtpReceiver, streams: Array<MediaStream>) {}
        }

        val pc = peerConnectionFactory.createPeerConnection(config, observer)!!

        // Thêm local tracks vào peer connection
        localVideoTrack?.let { pc.addTrack(it) }
        localAudioTrack?.let { pc.addTrack(it) }

        return pc
    }

    // ── Caller: tạo offer ─────────────────────────────────────────────────────
    fun call(sessionId: String) {
        scope.launch {
            peerConnection = createPeerConnection(sessionId)
            val constraints = MediaConstraints().apply {
                mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
                mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            }
            peerConnection!!.createOffer(object : SdpObserverAdapter() {
                override fun onCreateSuccess(sdp: SessionDescription) {
                    peerConnection!!.setLocalDescription(SdpObserverAdapter(), sdp)
                    scope.launch { callRepository.sendOffer(sessionId, sdp.description) }
                }
            }, constraints)

            // Lắng nghe answer từ callee
            callRepository.observeAnswer(sessionId).collectLatest { answerSdp ->
                answerSdp?.let {
                    val sessionDesc = SessionDescription(SessionDescription.Type.ANSWER, it)
                    peerConnection!!.setRemoteDescription(SdpObserverAdapter(), sessionDesc)
                }
            }
        }

        // Lắng nghe ICE candidates từ peer
        observeRemoteIceCandidates(sessionId)
    }

    // ── Callee: tạo answer ────────────────────────────────────────────────────
    fun answer(sessionId: String, offerSdp: String) {
        scope.launch {
            peerConnection = createPeerConnection(sessionId)
            val offer = SessionDescription(SessionDescription.Type.OFFER, offerSdp)
            peerConnection!!.setRemoteDescription(SdpObserverAdapter(), offer)

            peerConnection!!.createAnswer(object : SdpObserverAdapter() {
                override fun onCreateSuccess(sdp: SessionDescription) {
                    peerConnection!!.setLocalDescription(SdpObserverAdapter(), sdp)
                    scope.launch { callRepository.sendAnswer(sessionId, sdp.description) }
                }
            }, MediaConstraints())
        }
        observeRemoteIceCandidates(sessionId)
    }

    // ── Nhận ICE candidates từ Firestore ─────────────────────────────────────
    private fun observeRemoteIceCandidates(sessionId: String) {
        scope.launch {
            callRepository.observeIceCandidates(sessionId).collectLatest { candidates ->
                candidates.forEach { raw ->
                    val parts = raw.split("|")
                    if (parts.size == 3) {
                        val candidate = IceCandidate(parts[0], parts[1].toInt(), parts[2])
                        peerConnection?.addIceCandidate(candidate)
                    }
                }
            }
        }
    }

    // ── Kết thúc cuộc gọi ────────────────────────────────────────────────────
    fun endCall() {
        peerConnection?.close()
        peerConnection = null
        localVideoTrack?.dispose()
        localAudioTrack?.dispose()
        scope.coroutineContext.cancelChildren()
    }
}

// Adapter tránh phải override tất cả methods của SdpObserver
open class SdpObserverAdapter : SdpObserver {
    override fun onCreateSuccess(sdp: SessionDescription) {}
    override fun onSetSuccess() {}
    override fun onCreateFailure(error: String?) {}
    override fun onSetFailure(error: String?) {}
}
