package com.securechat.data.repository

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
<<<<<<< Updated upstream
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageException
=======
>>>>>>> Stashed changes
import com.securechat.data.local.dao.MessageDao
import com.securechat.data.mapper.toMessage
import com.securechat.data.mapper.toMessageEntity
import com.securechat.domain.model.*
import com.securechat.domain.repository.ChatRepository
import com.securechat.domain.repository.UserRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
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
                
                val sortedRooms = rooms.sortedByDescending { it.lastMessage?.createdAt ?: java.util.Date(0) }
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
            val directMembers = memberIds.distinct()
            if (!isGroup && directMembers.size == 2) {
                val firstUserId = directMembers.first()
                val existing = firestore.collection("rooms")
                    .whereEqualTo("isGroup", false)
                    .whereArrayContains("members", firstUserId)
                    .get()
                    .await()
                    .documents
                    .mapNotNull { it.toObject(ChatRoom::class.java)?.copy(id = it.id) }
                    .firstOrNull { room -> room.members.toSet() == directMembers.toSet() }

                if (existing != null) {
                    return Resource.Success(existing)
                }
            }

            val names = mutableMapOf<String, String>()
            val photos = mutableMapOf<String, String>()

            for (id in directMembers) {
                val userResult = userRepository.getUser(id)
                if (userResult is Resource.Success) {
                    names[id] = userResult.data.displayName
                    photos[id] = userResult.data.photoUrl ?: ""
                }
            }

            val room = ChatRoom(
                id       = UUID.randomUUID().toString(),
                name     = name,
                members  = directMembers,
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
        return try {
            val sender = firebaseAuth.currentUser
                ?: return Resource.Error("Chua dang nhap")

            val uri = runCatching { Uri.parse(fileUri) }
                .getOrElse { return Resource.Error("Duong dan file khong hop le") }

            val fileName = resolveDisplayName(uri) ?: "file_${System.currentTimeMillis()}"
            val fileSizeBytes = resolveFileSize(uri)
            val mimeType = resolveMimeType(uri)

            val validationResult = FileUploadValidator.validateForUpload(fileSizeBytes, mimeType)
            if (validationResult is Resource.Error) {
                return validationResult
            }

            val messageType = FileUploadValidator.resolveMessageType(mimeType)
                ?: return Resource.Error("Dinh dang file khong duoc ho tro")

            val safeFileName = fileName.replace(Regex("[^A-Za-z0-9._-]"), "_")
            val objectName = "${UUID.randomUUID()}_$safeFileName"
            val storageRef = storage.reference.child("chats/$roomId/files/$objectName")

            storageRef.putFile(uri).await()
            val downloadUrl = storageRef.downloadUrl.await().toString()

            val now = java.util.Date()
            val message = Message(
                id = UUID.randomUUID().toString(),
                roomId = roomId,
                senderId = sender.uid,
                senderName = sender.displayName ?: sender.email ?: "Nguoi dung",
                content = fileName,
                type = messageType,
                fileUrl = downloadUrl,
                fileName = fileName,
                deliveredTo = listOf(sender.uid),
                seenBy = listOf(sender.uid),
                createdAt = now
            )

            firestore.collection("rooms")
                .document(roomId)
                .collection("messages")
                .document(message.id)
                .set(message)
                .await()

            firestore.collection("rooms")
                .document(roomId)
                .update(
                    mapOf(
                        "lastMessage" to message,
                        "lastMessage.createdAt" to message.createdAt
                    )
                ).await()

            messageDao.insertMessage(message.toMessageEntity())
            Resource.Success(Unit)
        } catch (e: StorageException) {
            val errorMessage = when (e.errorCode) {
                StorageException.ERROR_RETRY_LIMIT_EXCEEDED,
                StorageException.ERROR_CANCELED -> "Tai file that bai do ket noi, vui long thu lai"
                StorageException.ERROR_QUOTA_EXCEEDED -> "Vuot gioi han luu tru"
                else -> e.localizedMessage ?: "Tai file that bai"
            }
            Resource.Error(errorMessage, e)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Gui file that bai", e)
        }
    }

    private fun resolveDisplayName(uri: Uri): String? {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0 && cursor.moveToFirst()) {
                    return cursor.getString(idx)
                }
            }
        return uri.lastPathSegment?.substringAfterLast('/')
    }

    private fun resolveFileSize(uri: Uri): Long {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)
            ?.use { cursor ->
                val idx = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (idx >= 0 && cursor.moveToFirst()) {
                    val value = cursor.getLong(idx)
                    if (value > 0L) return value
                }
            }

        return runCatching {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { fd -> fd.statSize } ?: -1L
        }.getOrDefault(-1L)
    }

    private fun resolveMimeType(uri: Uri): String? {
        val fromResolver = context.contentResolver.getType(uri)
        if (!fromResolver.isNullOrBlank()) return fromResolver
        val extension = resolveDisplayName(uri)
            ?.substringAfterLast('.', missingDelimiterValue = "")
            ?.lowercase()
            ?: return null

        return when (extension) {
            "jpg", "jpeg", "png", "gif", "webp" -> "image/$extension"
            "pdf" -> "application/pdf"
            "txt" -> "text/plain"
            "doc", "docx" -> "application/msword"
            "xls", "xlsx" -> "application/vnd.ms-excel"
            "mp4", "mov" -> "video/mp4"
            else -> null
        }
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

internal object FileUploadValidator {
    private const val MAX_FILE_BYTES: Long = 25L * 1024L * 1024L

    fun resolveMessageType(mimeType: String?): MessageType? {
        if (mimeType.isNullOrBlank()) return null
        return if (mimeType.startsWith("image/")) MessageType.IMAGE else if (isSupportedMime(mimeType)) MessageType.FILE else null
    }

    fun validateForUpload(fileSizeBytes: Long, mimeType: String?): Resource<Unit> {
        if (fileSizeBytes > MAX_FILE_BYTES) {
            return Resource.Error("File vuot qua gioi han 25MB")
        }
        if (resolveMessageType(mimeType) == null) {
            return Resource.Error("Dinh dang file khong duoc ho tro")
        }
        return Resource.Success(Unit)
    }

    private fun isSupportedMime(mimeType: String): Boolean {
        return mimeType.startsWith("application/") ||
            mimeType.startsWith("text/") ||
            mimeType.startsWith("audio/") ||
            mimeType.startsWith("video/") ||
            mimeType.startsWith("image/")
    }
}

