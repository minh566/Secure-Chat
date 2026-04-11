package com.securechat.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.securechat.domain.model.FriendRequest
import com.securechat.domain.model.FriendRequestStatus
import com.securechat.domain.model.Resource
import com.securechat.domain.model.User
import com.securechat.domain.repository.UserRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.launch
import java.util.Date
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth
) : UserRepository {

    private val usersRef get() = firestore.collection("users")
    private val requestsRef get() = firestore.collection("friend_requests")

    // Tìm kiếm user theo displayName
    override fun searchUsers(query: String): Flow<Resource<List<User>>> = callbackFlow {
        trySend(Resource.Loading)
        val listener = usersRef
            .orderBy("displayName")
            .startAt(query)
            .endAt(query + "\uf8ff")
            .limit(20)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    trySend(Resource.Error(error.message ?: "Lỗi tìm kiếm"))
                    return@addSnapshotListener
                }
                val users = snap?.documents
                    ?.mapNotNull { it.toObject(User::class.java)?.copy(uid = it.id) }
                    ?.filter { it.uid != firebaseAuth.currentUser?.uid }
                    ?: emptyList()
                trySend(Resource.Success(users))
            }
        awaitClose { listener.remove() }
    }

    // THỰC HIỆN HÀM LẤY DANH SÁCH BẠN BÈ ĐANG ONLINE
    override fun getActiveUsers(): Flow<Resource<List<User>>> = callbackFlow {
        trySend(Resource.Loading)
        val listener = usersRef
            .whereEqualTo("isOnline", true)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    trySend(Resource.Error(error.message ?: "Lỗi tải danh sách online"))
                    return@addSnapshotListener
                }
                val users = snap?.documents
                    ?.mapNotNull { it.toObject(User::class.java)?.copy(uid = it.id) }
                    ?.filter { it.uid != firebaseAuth.currentUser?.uid }
                    ?: emptyList()
                trySend(Resource.Success(users))
            }
        awaitClose { listener.remove() }
    }

    override suspend fun getUser(uid: String): Resource<User> {
        return try {
            val doc = usersRef.document(uid).get().await()
            val user = doc.toObject(User::class.java)?.copy(uid = doc.id)
                ?: return Resource.Error("Không tìm thấy người dùng")
            Resource.Success(user)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Lỗi tải thông tin")
        }
    }

    // Lắng nghe trạng thái online realtime
    override fun getUserOnlineStatus(uid: String): Flow<Boolean> = callbackFlow {
        val listener = usersRef.document(uid)
            .addSnapshotListener { snap, _ ->
                trySend(snap?.getBoolean("isOnline") ?: false)
            }
        awaitClose { listener.remove() }
    }

    // Cập nhật isOnline khi app vào/ra foreground
    override suspend fun setOnlineStatus(isOnline: Boolean): Resource<Unit> {
        return try {
            val uid = firebaseAuth.currentUser?.uid
                ?: return Resource.Error("Chưa đăng nhập")
            usersRef.document(uid)
                .update("isOnline", isOnline).await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Cập nhật trạng thái thất bại")
        }
    }

    // Lắng nghe các lời mời kết bạn đến
    override fun observeIncomingFriendRequests(): Flow<Resource<List<User>>> = callbackFlow {
        val currentUid = firebaseAuth.currentUser?.uid
        if (currentUid == null) {
            trySend(Resource.Error("Chưa đăng nhập"))
            close()
            return@callbackFlow
        }

        trySend(Resource.Loading)
        var fetchJob: Job? = null

        val listener = requestsRef
            .whereEqualTo("toUserId", currentUid)
            .whereEqualTo("status", FriendRequestStatus.PENDING.name)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    trySend(Resource.Error(error.message ?: "Lỗi tải lời mời kết bạn"))
                    return@addSnapshotListener
                }

                val senderIds = snap?.documents
                    ?.mapNotNull { it.getString("fromUserId") }
                    ?.distinct()
                    ?: emptyList()

                fetchJob?.cancel()
                fetchJob = launch {
                    if (senderIds.isEmpty()) {
                        trySend(Resource.Success(emptyList()))
                        return@launch
                    }

                    val users = mutableListOf<User>()
                    senderIds.chunked(10).forEach { chunk ->
                        val chunkSnap = usersRef
                            .whereIn(FieldPath.documentId(), chunk)
                            .get()
                            .await()
                        users += chunkSnap.documents.mapNotNull { doc ->
                            doc.toObject(User::class.java)?.copy(uid = doc.id)
                        }
                    }
                    trySend(Resource.Success(users))
                }
            }

        awaitClose {
            fetchJob?.cancel()
            listener.remove()
        }
    }

    // Gửi lời mời kết bạn
    override suspend fun sendFriendRequest(targetUserId: String): Resource<Unit> {
        val currentUid = firebaseAuth.currentUser?.uid ?: return Resource.Error("Chưa đăng nhập")
        if (currentUid == targetUserId) return Resource.Error("Không thể kết bạn với chính mình")

        return try {
            val targetDoc = usersRef.document(targetUserId).get().await()
            if (!targetDoc.exists()) return Resource.Error("Người dùng không tồn tại")

            val acceptedRelation = requestsRef
                .whereEqualTo("status", FriendRequestStatus.ACCEPTED.name)
                .get()
                .await()
                .documents
                .any { doc ->
                    val from = doc.getString("fromUserId")
                    val to = doc.getString("toUserId")
                    (from == currentUid && to == targetUserId) || (from == targetUserId && to == currentUid)
                }
            if (acceptedRelation) return Resource.Error("Hai bạn đã là bạn bè")

            val pendingForward = requestsRef
                .whereEqualTo("fromUserId", currentUid)
                .whereEqualTo("toUserId", targetUserId)
                .whereEqualTo("status", FriendRequestStatus.PENDING.name)
                .limit(1)
                .get()
                .await()
            if (!pendingForward.isEmpty) return Resource.Error("Bạn đã gửi lời mời trước đó")

            val pendingReverse = requestsRef
                .whereEqualTo("fromUserId", targetUserId)
                .whereEqualTo("toUserId", currentUid)
                .whereEqualTo("status", FriendRequestStatus.PENDING.name)
                .limit(1)
                .get()
                .await()
            if (!pendingReverse.isEmpty) return Resource.Error("Người này đã gửi lời mời cho bạn, hãy vào mục lời mời để chấp nhận")

            val now = Date()
            val request = FriendRequest(
                id = UUID.randomUUID().toString(),
                fromUserId = currentUid,
                toUserId = targetUserId,
                status = FriendRequestStatus.PENDING,
                createdAt = now,
                updatedAt = now
            )

            requestsRef.document(request.id).set(
                mapOf(
                    "id" to request.id,
                    "fromUserId" to request.fromUserId,
                    "toUserId" to request.toUserId,
                    "status" to request.status.name,
                    "createdAt" to request.createdAt,
                    "updatedAt" to request.updatedAt
                )
            ).await()

            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Gửi lời mời thất bại")
        }
    }

    // Chấp nhận lời mời kết bạn
    override suspend fun acceptFriendRequest(fromUserId: String): Resource<Unit> {
        val currentUid = firebaseAuth.currentUser?.uid ?: return Resource.Error("Chưa đăng nhập")

        return try {
            val req = requestsRef
                .whereEqualTo("fromUserId", fromUserId)
                .whereEqualTo("toUserId", currentUid)
                .whereEqualTo("status", FriendRequestStatus.PENDING.name)
                .limit(1)
                .get()
                .await()
                .documents
                .firstOrNull()
                ?: return Resource.Error("Không tìm thấy lời mời kết bạn")

            req.reference.update(
                mapOf(
                    "status" to FriendRequestStatus.ACCEPTED.name,
                    "updatedAt" to Date()
                )
            ).await()

            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Chấp nhận lời mời thất bại")
        }
    }

    // Từ chối lời mời kết bạn
    override suspend fun rejectFriendRequest(fromUserId: String): Resource<Unit> {
        val currentUid = firebaseAuth.currentUser?.uid ?: return Resource.Error("Chưa đăng nhập")

        return try {
            val req = requestsRef
                .whereEqualTo("fromUserId", fromUserId)
                .whereEqualTo("toUserId", currentUid)
                .whereEqualTo("status", FriendRequestStatus.PENDING.name)
                .limit(1)
                .get()
                .await()
                .documents
                .firstOrNull()
                ?: return Resource.Error("Không tìm thấy lời mời kết bạn")

            req.reference.update(
                mapOf(
                    "status" to FriendRequestStatus.REJECTED.name,
                    "updatedAt" to Date()
                )
            ).await()

            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Từ chối lời mời thất bại")
        }
    }
}
