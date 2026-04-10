package com.securechat.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.securechat.data.remote.signaling.SignalingApiClient
import com.securechat.data.remote.signaling.SignalingEnvelope
import com.securechat.data.remote.signaling.SignalingWebSocketClient
import com.securechat.domain.model.*
import com.securechat.domain.repository.AuthRepository
import com.securechat.domain.repository.CallRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firestore làm Signaling Server cho WebRTC:
 *
 * Cấu trúc Firestore:
 * calls/{sessionId}
 *   ├── callerId, calleeId, status, type
 *   ├── offer:  { sdp: "..." }
 *   ├── answer: { sdp: "..." }
 *   └── iceCandidates/
 *         ├── caller/ → {0: "...", 1: "..."}
 *         └── callee/ → {0: "...", 1: "..."}
 */
@Singleton
class CallRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val authRepository: AuthRepository,
    private val signalingApiClient: SignalingApiClient,
    private val signalingWebSocketClient: SignalingWebSocketClient
) : CallRepository {

    private val callsRef get() = firestore.collection("calls")

    private fun roleIceRef(sessionId: String, role: String) = callsRef.document(sessionId)
        .collection("iceCandidates")
        .document(role)
        .collection("items")

    private fun ensureSocketConnected() {
        val userId = authRepository.currentUser?.uid.orEmpty()
        if (userId.isNotBlank()) {
            signalingWebSocketClient.connect(userId)
        }
    }

    // ── Lắng nghe cuộc gọi đến ───────────────────────────────────────────────
    override fun observeIncomingCall(userId: String): Flow<CallSession?> = callbackFlow {
        val listener = callsRef
            .whereEqualTo("calleeId", userId)
            .whereEqualTo("status", CallStatus.RINGING.name)
            .addSnapshotListener { snap, err ->
                if (err != null) { trySend(null); return@addSnapshotListener }
                val session = snap?.documents?.firstOrNull()
                    ?.toObject(CallSession::class.java)
                trySend(session)
            }
        awaitClose { listener.remove() }
    }

    // ── Tạo cuộc gọi ─────────────────────────────────────────────────────────
    override suspend fun initiateCall(session: CallSession): Resource<Unit> {
        return try {
            callsRef.document(session.id).set(session).await()
            signalingApiClient.createCall(session)
            ensureSocketConnected()
            signalingWebSocketClient.send(
                SignalingEnvelope(
                    type = "incoming_call",
                    sessionId = session.id,
                    fromUserId = session.callerId,
                    toUserId = session.calleeId,
                    status = CallStatus.RINGING.name
                )
            )
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Không thể gọi")
        }
    }

    override suspend fun acceptCall(sessionId: String): Resource<Unit> {
        return try {
            callsRef.document(sessionId)
                .update("status", CallStatus.ACCEPTED.name).await()
            signalingApiClient.updateCallStatus(sessionId, CallStatus.ACCEPTED.name)
            Resource.Success(Unit)
        } catch (e: Exception) { Resource.Error(e.localizedMessage ?: "Lỗi") }
    }

    override suspend fun declineCall(sessionId: String): Resource<Unit> {
        return try {
            callsRef.document(sessionId)
                .update("status", CallStatus.DECLINED.name).await()
            signalingApiClient.updateCallStatus(sessionId, CallStatus.DECLINED.name)
            Resource.Success(Unit)
        } catch (e: Exception) { Resource.Error(e.localizedMessage ?: "Lỗi") }
    }

    override suspend fun endCall(sessionId: String): Resource<Unit> {
        return try {
            callsRef.document(sessionId)
                .update("status", CallStatus.ENDED.name).await()
            signalingApiClient.updateCallStatus(sessionId, CallStatus.ENDED.name)
            Resource.Success(Unit)
        } catch (e: Exception) { Resource.Error(e.localizedMessage ?: "Lỗi") }
    }

    override fun observeCallStatus(sessionId: String): Flow<CallStatus?> = callbackFlow {
        val listener = callsRef.document(sessionId)
            .addSnapshotListener { snap, _ ->
                val raw = snap?.getString("status")
                val status = raw?.let { value ->
                    runCatching { CallStatus.valueOf(value) }.getOrNull()
                }
                trySend(status)
            }
        awaitClose { listener.remove() }
    }.distinctUntilChanged()

    // ── WebRTC Signaling: SDP Offer ───────────────────────────────────────────
    override suspend fun sendOffer(sessionId: String, sdp: String): Resource<Unit> {
        return try {
            ensureSocketConnected()
            callsRef.document(sessionId)
                .update("offer", mapOf("sdp" to sdp)).await()
            signalingWebSocketClient.send(
                SignalingEnvelope(
                    type = "offer",
                    sessionId = sessionId,
                    fromUserId = authRepository.currentUser?.uid.orEmpty(),
                    sdp = sdp
                )
            )
            Resource.Success(Unit)
        } catch (e: Exception) { Resource.Error(e.localizedMessage ?: "Lỗi gửi offer") }
    }

    override fun observeOffer(sessionId: String): Flow<String?> {
        ensureSocketConnected()
        val wsFlow = signalingWebSocketClient.events
            .filter { it.type == "offer" && it.sessionId == sessionId }
            .map { it.sdp }

        val firestoreFlow = callbackFlow {
            val listener = callsRef.document(sessionId)
                .addSnapshotListener { snap, _ ->
                    @Suppress("UNCHECKED_CAST")
                    val offerMap = snap?.get("offer") as? Map<String, String>
                    trySend(offerMap?.get("sdp"))
                }
            awaitClose { listener.remove() }
        }

        return merge(wsFlow, firestoreFlow).distinctUntilChanged()
    }

    // ── WebRTC Signaling: SDP Answer ──────────────────────────────────────────
    override suspend fun sendAnswer(sessionId: String, sdp: String): Resource<Unit> {
        return try {
            ensureSocketConnected()
            callsRef.document(sessionId)
                .update("answer", mapOf("sdp" to sdp)).await()
            signalingWebSocketClient.send(
                SignalingEnvelope(
                    type = "answer",
                    sessionId = sessionId,
                    fromUserId = authRepository.currentUser?.uid.orEmpty(),
                    sdp = sdp
                )
            )
            Resource.Success(Unit)
        } catch (e: Exception) { Resource.Error(e.localizedMessage ?: "Lỗi gửi answer") }
    }

    override fun observeAnswer(sessionId: String): Flow<String?> {
        ensureSocketConnected()
        val wsFlow = signalingWebSocketClient.events
            .filter { it.type == "answer" && it.sessionId == sessionId }
            .map { it.sdp }

        val firestoreFlow = callbackFlow {
            val listener = callsRef.document(sessionId)
                .addSnapshotListener { snap, _ ->
                    @Suppress("UNCHECKED_CAST")
                    val answerMap = snap?.get("answer") as? Map<String, String>
                    trySend(answerMap?.get("sdp"))
                }
            awaitClose { listener.remove() }
        }

        return merge(wsFlow, firestoreFlow).distinctUntilChanged()
    }

    // ── WebRTC Signaling: ICE Candidates ─────────────────────────────────────
    override suspend fun sendIceCandidate(sessionId: String, role: String, candidate: String): Resource<Unit> {
        return try {
            ensureSocketConnected()
            roleIceRef(sessionId, role)
                .add(mapOf("candidate" to candidate)).await()
            signalingWebSocketClient.send(
                SignalingEnvelope(
                    type = "ice_candidate",
                    sessionId = sessionId,
                    fromUserId = authRepository.currentUser?.uid.orEmpty(),
                    candidate = candidate,
                    role = role
                )
            )
            Resource.Success(Unit)
        } catch (e: Exception) { Resource.Error(e.localizedMessage ?: "Lỗi gửi ICE") }
    }

    override fun observeIceCandidates(sessionId: String, role: String): Flow<List<String>> {
        ensureSocketConnected()

        val wsFlow = signalingWebSocketClient.events
            .filter {
                it.type == "ice_candidate" &&
                    it.sessionId == sessionId &&
                    it.role == role
            }
            .mapNotNull { it.candidate }
            .runningFold(emptyList<String>()) { acc, candidate ->
                (acc + candidate).distinct()
            }

        val firestoreFlow = callbackFlow {
            val listener = roleIceRef(sessionId, role)
                .addSnapshotListener { snap, _ ->
                    val candidates = snap?.documents?.mapNotNull { it.getString("candidate") } ?: emptyList()
                    trySend(candidates)
                }
            awaitClose { listener.remove() }
        }

        return merge(wsFlow, firestoreFlow).distinctUntilChanged()
    }
}
