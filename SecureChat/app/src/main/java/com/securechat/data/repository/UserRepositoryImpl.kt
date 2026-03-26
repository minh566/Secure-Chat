package com.securechat.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.securechat.domain.model.Resource
import com.securechat.domain.model.User
import com.securechat.domain.repository.UserRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth
) : UserRepository {

    private val usersRef get() = firestore.collection("users")

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
}
