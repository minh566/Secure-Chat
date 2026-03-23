package com.securechat.domain.repository

import com.securechat.domain.model.*
import kotlinx.coroutines.flow.Flow

// ── Auth Repository ───────────────────────────────────────────────────────────
// Interface này nằm ở Domain — không biết Firebase tồn tại
interface AuthRepository {
    val currentUser: User?
    fun isLoggedIn(): Boolean
    suspend fun signIn(email: String, password: String): AuthResult
    suspend fun signUp(email: String, password: String, displayName: String): AuthResult
    suspend fun signOut()
    suspend fun updateProfile(displayName: String, photoUrl: String?): Resource<Unit>
    suspend fun updateFcmToken(token: String): Resource<Unit>
}

// ── Chat Repository ───────────────────────────────────────────────────────────
interface ChatRepository {
    // Rooms
    fun getChatRooms(userId: String): Flow<Resource<List<ChatRoom>>>
    suspend fun createRoom(name: String, memberIds: List<String>, isGroup: Boolean): Resource<ChatRoom>
    suspend fun getChatRoom(roomId: String): Resource<ChatRoom>

    // Messages — trả Flow để realtime update từ Firestore
    fun getMessages(roomId: String): Flow<Resource<List<Message>>>
    suspend fun sendMessage(message: Message): Resource<Unit>
    suspend fun sendFile(roomId: String, fileUri: String, type: MessageType): Resource<Unit>
    suspend fun markAsRead(roomId: String, userId: String): Resource<Unit>
}

// ── User Repository ───────────────────────────────────────────────────────────
interface UserRepository {
    fun searchUsers(query: String): Flow<Resource<List<User>>>
    suspend fun getUser(uid: String): Resource<User>
    fun getUserOnlineStatus(uid: String): Flow<Boolean>
    suspend fun setOnlineStatus(isOnline: Boolean): Resource<Unit>
}

// ── Call Repository ───────────────────────────────────────────────────────────
interface CallRepository {
    fun observeIncomingCall(userId: String): Flow<CallSession?>
    suspend fun initiateCall(session: CallSession): Resource<Unit>
    suspend fun acceptCall(sessionId: String): Resource<Unit>
    suspend fun declineCall(sessionId: String): Resource<Unit>
    suspend fun endCall(sessionId: String): Resource<Unit>

    // WebRTC Signaling qua Firestore
    suspend fun sendOffer(sessionId: String, sdp: String): Resource<Unit>
    suspend fun sendAnswer(sessionId: String, sdp: String): Resource<Unit>
    suspend fun sendIceCandidate(sessionId: String, candidate: String): Resource<Unit>
    fun observeOffer(sessionId: String): Flow<String?>
    fun observeAnswer(sessionId: String): Flow<String?>
    fun observeIceCandidates(sessionId: String): Flow<List<String>>
}
