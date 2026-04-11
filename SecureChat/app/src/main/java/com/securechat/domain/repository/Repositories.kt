package com.securechat.domain.repository

import com.securechat.domain.model.*
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    fun getChatRooms(userId: String): Flow<Resource<List<ChatRoom>>>
    suspend fun createRoom(
        name: String,
        memberIds: List<String>,
        isGroup: Boolean,
        roomImageUri: String? = null
    ): Resource<ChatRoom>
    suspend fun getChatRoom(roomId: String): Resource<ChatRoom>
    suspend fun addMembersToRoom(roomId: String, memberIds: List<String>): Resource<Unit>
    suspend fun deleteChatRoom(roomId: String): Resource<Unit> // THÊM DÒNG NÀY

    fun getMessages(roomId: String): Flow<Resource<List<Message>>>
    suspend fun sendMessage(message: Message): Resource<Unit>
    suspend fun deleteMessage(roomId: String, messageId: String): Resource<Unit>
    suspend fun sendFile(
        roomId: String,
        fileUri: String,
        type: MessageType,
        onProgress: (Int) -> Unit = {}
    ): Resource<Unit>
    suspend fun cacheAttachment(roomId: String, message: Message): Resource<Message>
    suspend fun setMessageReaction(roomId: String, messageId: String, userId: String, emoji: String): Resource<Unit>
    suspend fun removeMessageReaction(roomId: String, messageId: String, userId: String): Resource<Unit>
    suspend fun markAsRead(roomId: String, userId: String): Resource<Unit>
    suspend fun markMessagesDelivered(roomId: String, userId: String): Resource<Unit>
    suspend fun markMessagesSeen(roomId: String, userId: String): Resource<Unit>
}

interface AuthRepository {
    val currentUser: User?
    fun isLoggedIn(): Boolean
    suspend fun signIn(email: String, password: String): AuthResult
    suspend fun signUp(email: String, password: String, displayName: String): AuthResult
    suspend fun signOut()
    suspend fun updateProfile(displayName: String, photoUrl: String?): Resource<Unit>
    suspend fun updateFcmToken(token: String): Resource<Unit>
}

interface UserRepository {
    fun searchUsers(query: String): Flow<Resource<List<User>>>
    fun getActiveUsers(): Flow<Resource<List<User>>>
    suspend fun getUser(uid: String): Resource<User>
    fun getUserOnlineStatus(uid: String): Flow<Boolean>
    suspend fun setOnlineStatus(isOnline: Boolean): Resource<Unit>
    fun observeIncomingFriendRequests(): Flow<Resource<List<User>>>
    suspend fun sendFriendRequest(targetUserId: String): Resource<Unit>
    suspend fun acceptFriendRequest(fromUserId: String): Resource<Unit>
    suspend fun rejectFriendRequest(fromUserId: String): Resource<Unit>
}

interface CallRepository {
    fun observeIncomingCall(userId: String): Flow<CallSession?>
    suspend fun initiateCall(session: CallSession): Resource<Unit>
    suspend fun acceptCall(sessionId: String): Resource<Unit>
    suspend fun declineCall(sessionId: String): Resource<Unit>
    suspend fun endCall(sessionId: String): Resource<Unit>
    fun observeCallStatus(sessionId: String): Flow<CallStatus?>
    fun observeOffer(sessionId: String): Flow<String?>
    fun observeAnswer(sessionId: String): Flow<String?>
    fun observeIceCandidates(sessionId: String, role: String): Flow<List<String>>
    suspend fun sendOffer(sessionId: String, sdp: String): Resource<Unit>
    suspend fun sendAnswer(sessionId: String, sdp: String): Resource<Unit>
    suspend fun sendIceCandidate(sessionId: String, role: String, candidate: String): Resource<Unit>
}
