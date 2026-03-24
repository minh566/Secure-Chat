package com.securechat.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.securechat.data.local.dao.MessageDao
import com.securechat.data.mapper.toMessage
import com.securechat.data.mapper.toMessageEntity
import com.securechat.domain.model.*
import com.securechat.domain.repository.ChatRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage,
    private val messageDao: MessageDao
) : ChatRepository {

    override fun getChatRooms(userId: String): Flow<Resource<List<ChatRoom>>> = callbackFlow {
        trySend(Resource.Loading)
        // SỬA ĐỔI: Tạm thời bỏ .orderBy để không yêu cầu Composite Index
        val listener = firestore.collection("rooms")
            .whereArrayContains("members", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Resource.Error(error.message ?: "Lỗi tải phòng chat"))
                    return@addSnapshotListener
                }
                val rooms = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(ChatRoom::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                
                // Sắp xếp thủ công bằng code Kotlin thay vì dùng Query (để tránh lỗi Index)
                val sortedRooms = rooms.sortedByDescending { it.lastMessage?.createdAt }
                
                trySend(Resource.Success(sortedRooms))
            }
        awaitClose { listener.remove() }
    }

    override suspend fun createRoom(
        name: String,
        memberIds: List<String>,
        isGroup: Boolean
    ): Resource<ChatRoom> {
        return try {
            val room = ChatRoom(
                id       = UUID.randomUUID().toString(),
                name     = name,
                members  = memberIds,
                isGroup  = isGroup
            )
            firestore.collection("rooms").document(room.id).set(room).await()
            Resource.Success(room)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Tạo phòng thất bại")
        }
    }

    override suspend fun getChatRoom(roomId: String): Resource<ChatRoom> {
        return try {
            val doc = firestore.collection("rooms").document(roomId).get().await()
            val room = doc.toObject(ChatRoom::class.java)?.copy(id = doc.id)
                ?: return Resource.Error("Không tìm thấy phòng")
            Resource.Success(room)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Lỗi tải phòng")
        }
    }

    override fun getMessages(roomId: String): Flow<Resource<List<Message>>> = flow {
        emit(Resource.Loading)
        val cached = messageDao.getMessagesByRoom(roomId).firstOrNull()
        if (cached != null && cached.isNotEmpty()) emit(Resource.Success(cached.map { it.toMessage() }))

        val remoteFlow: Flow<Resource<List<Message>>> = callbackFlow {
            val listener = firestore.collection("rooms")
                .document(roomId)
                .collection("messages")
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(Resource.Error(error.message ?: "Lỗi tải tin nhắn"))
                        return@addSnapshotListener
                    }
                    val messages = snapshot?.documents?.mapNotNull { doc ->
                        doc.toObject(Message::class.java)?.copy(id = doc.id)
                    } ?: emptyList()
                    trySend(Resource.Success(messages))
                }
            awaitClose { listener.remove() }
        }

        remoteFlow.collect { result ->
            if (result is Resource.Success) {
                messageDao.insertMessages(result.data.map { it.toMessageEntity() })
            }
            emit(result)
        }
    }

    override suspend fun sendMessage(message: Message): Resource<Unit> {
        return try {
            firestore.collection("rooms")
                .document(message.roomId)
                .collection("messages")
                .document(message.id)
                .set(message)
                .await()

            firestore.collection("rooms")
                .document(message.roomId)
                .update(
                    mapOf(
                        "lastMessage" to message,
                        "lastMessage.createdAt" to message.createdAt
                    )
                ).await()

            messageDao.insertMessage(message.toMessageEntity())
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Gửi tin nhắn thất bại")
        }
    }

    override suspend fun sendFile(roomId: String, fileUri: String, type: MessageType): Resource<Unit> {
        return Resource.Success(Unit)
    }

    override suspend fun markAsRead(roomId: String, userId: String): Resource<Unit> {
        return try {
            firestore.collection("rooms")
                .document(roomId)
                .update("unreadCount.$userId", 0)
                .await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Lỗi đánh dấu đã đọc")
        }
    }
}
