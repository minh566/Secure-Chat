package com.securechat.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.securechat.domain.model.*
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
    private val firestore: FirebaseFirestore
) : CallRepository {

    private val callsRef get() = firestore.collection("calls")

    private fun roleIceRef(sessionId: String, role: String) = callsRef.document(sessionId)
        .collection("iceCandidates")
        .document(role)
        .collection("items")

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
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Không thể gọi")
        }
    }

    override suspend fun acceptCall(sessionId: String): Resource<Unit> {
        return try {
            callsRef.document(sessionId)
                .update("status", CallStatus.ACCEPTED.name).await()
            Resource.Success(Unit)
        } catch (e: Exception) { Resource.Error(e.localizedMessage ?: "Lỗi") }
    }

    override suspend fun declineCall(sessionId: String): Resource<Unit> {
        return try {
            callsRef.document(sessionId)
                .update("status", CallStatus.DECLINED.name).await()
            Resource.Success(Unit)
        } catch (e: Exception) { Resource.Error(e.localizedMessage ?: "Lỗi") }
    }

    override suspend fun endCall(sessionId: String): Resource<Unit> {
        return try {
            callsRef.document(sessionId)
                .update("status", CallStatus.ENDED.name).await()
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
            callsRef.document(sessionId)
                .update("offer", mapOf("sdp" to sdp)).await()
            Resource.Success(Unit)
        } catch (e: Exception) { Resource.Error(e.localizedMessage ?: "Lỗi gửi offer") }
    }

    override fun observeOffer(sessionId: String): Flow<String?> = callbackFlow {
        val listener = callsRef.document(sessionId)
            .addSnapshotListener { snap, _ ->
                @Suppress("UNCHECKED_CAST")
                val offerMap = snap?.get("offer") as? Map<String, String>
                trySend(offerMap?.get("sdp"))
            }
        awaitClose { listener.remove() }
    }

    // ── WebRTC Signaling: SDP Answer ──────────────────────────────────────────
    override suspend fun sendAnswer(sessionId: String, sdp: String): Resource<Unit> {
        return try {
            callsRef.document(sessionId)
                .update("answer", mapOf("sdp" to sdp)).await()
            Resource.Success(Unit)
        } catch (e: Exception) { Resource.Error(e.localizedMessage ?: "Lỗi gửi answer") }
    }

    override fun observeAnswer(sessionId: String): Flow<String?> = callbackFlow {
        val listener = callsRef.document(sessionId)
            .addSnapshotListener { snap, _ ->
                @Suppress("UNCHECKED_CAST")
                val answerMap = snap?.get("answer") as? Map<String, String>
                trySend(answerMap?.get("sdp"))
            }
        awaitClose { listener.remove() }
    }

    // ── WebRTC Signaling: ICE Candidates ─────────────────────────────────────
    override suspend fun sendIceCandidate(sessionId: String, role: String, candidate: String): Resource<Unit> {
        return try {
            roleIceRef(sessionId, role)
                .add(mapOf("candidate" to candidate)).await()
            Resource.Success(Unit)
        } catch (e: Exception) { Resource.Error(e.localizedMessage ?: "Lỗi gửi ICE") }
    }

    override fun observeIceCandidates(sessionId: String, role: String): Flow<List<String>> {
        val roleFlow = callbackFlow {
            val listener = roleIceRef(sessionId, role)
                .addSnapshotListener { snap, _ ->
                    val candidates = snap?.documents?.mapNotNull { it.getString("candidate") } ?: emptyList()
                    trySend(candidates)
                }
            awaitClose { listener.remove() }
        }

        // Legacy fallback while old clients still write to the flat collection.
        val legacyFlow = callbackFlow {
            val listener = callsRef.document(sessionId)
                .collection("iceCandidates")
                .addSnapshotListener { snap, _ ->
                    val candidates = snap?.documents?.mapNotNull { it.getString("candidate") } ?: emptyList()
                    trySend(candidates)
                }
            awaitClose { listener.remove() }
        }

        return combine(roleFlow, legacyFlow) { roleCandidates, legacyCandidates ->
            (roleCandidates + legacyCandidates).distinct()
        }.distinctUntilChanged()
    }
}
