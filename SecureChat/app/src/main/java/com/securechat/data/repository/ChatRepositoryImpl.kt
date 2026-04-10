package com.securechat.data.repository

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.securechat.data.local.dao.MessageDao
import com.securechat.data.mapper.toMessage
import com.securechat.data.mapper.toMessageEntity
import com.securechat.domain.model.*
import com.securechat.domain.repository.ChatRepository
import com.securechat.domain.repository.UserRepository
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
    private val messageDao: MessageDao,
    private val userRepository: UserRepository
) : ChatRepository {

    override fun getChatRooms(userId: String): Flow<Resource<List<ChatRoom>>> = callbackFlow {
        trySend(Resource.Loading)
        val listener = firestore.collection("rooms")
            .whereArrayContains("members", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Resource.Error(error.message ?: "Lỗi tải phòng chat"))
                    return@addSnapshotListener
                }
                val rooms = snapshot?.documents?.mapNotNull { doc ->
                    runCatching {
                        doc.toObject(ChatRoom::class.java)?.copy(id = doc.id)
                    }.getOrNull()
                } ?: emptyList()
                
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
            val names = mutableMapOf<String, String>()
            val photos = mutableMapOf<String, String>()
            
            for (id in memberIds) {
                val userResult = userRepository.getUser(id)
                if (userResult is Resource.Success) {
                    names[id] = userResult.data.displayName
                    photos[id] = userResult.data.photoUrl ?: ""
                }
            }

            val room = ChatRoom(
                id       = UUID.randomUUID().toString(),
                name     = name,
                members  = memberIds,
                memberNames = names,
                memberPhotos = photos,
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
            val room = runCatching {
                doc.toObject(ChatRoom::class.java)?.copy(id = doc.id)
            }.getOrNull() ?: return Resource.Error("Không tìm thấy phòng hoặc dữ liệu phòng không hợp lệ")
            Resource.Success(room)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Lỗi tải phòng")
        }
    }

    override suspend fun addMembersToRoom(roomId: String, memberIds: List<String>): Resource<Unit> {
        if (memberIds.isEmpty()) return Resource.Error("Chưa chọn thành viên để thêm")

        return try {
            val roomRef = firestore.collection("rooms").document(roomId)
            val roomDoc = roomRef.get().await()
            val room = roomDoc.toObject(ChatRoom::class.java)?.copy(id = roomDoc.id)
                ?: return Resource.Error("Không tìm thấy phòng chat")

            val existingMembers = room.members.toSet()
            val newMemberIds = memberIds.filter { it !in existingMembers }.distinct()
            if (newMemberIds.isEmpty()) return Resource.Success(Unit)

            val updatedNames = room.memberNames.toMutableMap()
            val updatedPhotos = room.memberPhotos.toMutableMap()
            val updatedUnread = room.unreadCount.toMutableMap()

            newMemberIds.forEach { uid ->
                val userResult = userRepository.getUser(uid)
                if (userResult is Resource.Success) {
                    updatedNames[uid] = userResult.data.displayName
                    updatedPhotos[uid] = userResult.data.photoUrl ?: ""
                }
                updatedUnread.putIfAbsent(uid, 0)
            }

            val mergedMembers = (room.members + newMemberIds).distinct()
            roomRef.update(
                mapOf(
                    "members" to mergedMembers,
                    "memberNames" to updatedNames,
                    "memberPhotos" to updatedPhotos,
                    "unreadCount" to updatedUnread,
                    "isGroup" to (room.isGroup || mergedMembers.size > 2)
                )
            ).await()

            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Thêm thành viên thất bại")
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
            val normalizedMessage = message.copy(
                deliveredTo = (message.deliveredTo + message.senderId).distinct(),
                seenBy = (message.seenBy + message.senderId).distinct()
            )

            firestore.collection("rooms")
                .document(normalizedMessage.roomId)
                .collection("messages")
                .document(normalizedMessage.id)
                .set(normalizedMessage)
                .await()

            firestore.collection("rooms")
                .document(normalizedMessage.roomId)
                .update(
                    mapOf(
                        "lastMessage" to normalizedMessage,
                        "lastMessage.createdAt" to normalizedMessage.createdAt
                    )
                ).await()

            messageDao.insertMessage(normalizedMessage.toMessageEntity())
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Gửi tin nhắn thất bại")
        }
    }

    // THỰC HIỆN HÀM XÓA TIN NHẮN
    override suspend fun deleteMessage(roomId: String, messageId: String): Resource<Unit> {
        return try {
            // Xóa trên Firestore
            firestore.collection("rooms")
                .document(roomId)
                .collection("messages")
                .document(messageId)
                .delete()
                .await()

            // Xóa ở Local Database (Room)
            messageDao.deleteMessage(messageId)
            
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Xóa tin nhắn thất bại")
        }
    }

    override suspend fun sendFile(roomId: String, fileUri: String, type: MessageType): Resource<Unit> {
        return Resource.Success(Unit)
    }

    override suspend fun deleteChatRoom(roomId: String): Resource<Unit> {
        return try {
            val roomRef = firestore.collection("rooms").document(roomId)
            val messagesRef = roomRef.collection("messages")
            val batchSize = 200L

            while (true) {
                val snapshot = messagesRef.limit(batchSize).get().await()
                if (snapshot.isEmpty) break

                val batch = firestore.batch()
                snapshot.documents.forEach { doc -> batch.delete(doc.reference) }
                batch.commit().await()

                if (snapshot.size() < batchSize.toInt()) break
            }

            roomRef.delete().await()
            messageDao.deleteRoomMessages(roomId)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Xóa phòng chat thất bại")
        }
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

    override suspend fun markMessagesDelivered(roomId: String, userId: String): Resource<Unit> {
        return updateMessageReceipt(roomId, userId, isSeen = false)
    }

    override suspend fun markMessagesSeen(roomId: String, userId: String): Resource<Unit> {
        return updateMessageReceipt(roomId, userId, isSeen = true)
    }

    private suspend fun updateMessageReceipt(roomId: String, userId: String, isSeen: Boolean): Resource<Unit> {
        return try {
            val roomRef = firestore.collection("rooms").document(roomId)
            val snapshot = roomRef.collection("messages")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(100)
                .get()
                .await()

            val docsToUpdate = snapshot.documents.filter { doc ->
                val senderId = doc.getString("senderId")
                val delivered = (doc.get("deliveredTo") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                val seen = (doc.get("seenBy") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                senderId != null && senderId != userId && (userId !in delivered || (isSeen && userId !in seen))
            }

            if (docsToUpdate.isNotEmpty()) {
                val batch = firestore.batch()
                docsToUpdate.forEach { doc ->
                    batch.update(doc.reference, "deliveredTo", FieldValue.arrayUnion(userId))
                    if (isSeen) {
                        batch.update(doc.reference, "seenBy", FieldValue.arrayUnion(userId))
                        batch.update(doc.reference, "isRead", true)
                    }
                }
                batch.commit().await()
            }

            val roomDoc = roomRef.get().await()
            val lastMessageDoc = roomDoc.get("lastMessage") as? Map<*, *>
            val lastSenderId = lastMessageDoc?.get("senderId") as? String
            if (lastSenderId != null && lastSenderId != userId) {
                val roomUpdates = mutableMapOf<String, Any>(
                    "lastMessage.deliveredTo" to FieldValue.arrayUnion(userId)
                )
                if (isSeen) {
                    roomUpdates["lastMessage.seenBy"] = FieldValue.arrayUnion(userId)
                    roomUpdates["lastMessage.isRead"] = true
                }
                roomRef.update(roomUpdates).await()
            }

            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Lỗi cập nhật trạng thái tin nhắn")
        }
    }
}
