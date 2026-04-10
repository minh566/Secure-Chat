package com.securechat.data.repository

import com.securechat.domain.model.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FakeRepository @Inject constructor() {

    private val _users = listOf(
        User("1", "Alex Johnson", "alex@example.com", isOnline = true),
        User("2", "Maria Garcia", "maria@example.com", isOnline = true),
        User("3", "James Smith", "james@example.com", isOnline = false),
        User("4", "Linda Williams", "linda@example.com", isOnline = true),
        User("5", "Robert Brown", "robert@example.com", isOnline = false)
    )

    private val _rooms = MutableStateFlow(
        _users.map { user ->
            ChatRoom(
                id = "room_${user.uid}",
                name = user.displayName,
                members = listOf("me", user.uid),
                lastMessage = Message(
                    id = UUID.randomUUID().toString(),
                    content = "Hey there! How is it going?",
                    createdAt = Date(),
                    senderId = user.uid,
                    senderName = user.displayName
                ),
                unreadCount = mapOf("me" to (0..3).random())
            )
        }
    )

    private val _messages = MutableStateFlow<Map<String, List<Message>>>(
        _rooms.value.associate { room ->
            val user = _users.find { it.uid == room.members.find { it != "me" } } ?: _users.first()
            room.id to List(15) { i ->
                val senderId = if (i % 2 == 0) "me" else room.members.find { it != "me" } ?: ""
                val senderName = if (senderId == "me") "Demo User" else user.displayName
                Message(
                    id = UUID.randomUUID().toString(),
                    roomId = room.id,
                    senderId = senderId,
                    senderName = senderName,
                    content = "Message $i in ${room.name}",
                    createdAt = Date(System.currentTimeMillis() - (15 - i) * 60000)
                )
            }
        }
    )

    fun getChatRooms(): Flow<Resource<List<ChatRoom>>> = flow {
        emit(Resource.Loading)
        delay(500)
        _rooms.collect { emit(Resource.Success(it)) }
    }

    fun getMessages(roomId: String): Flow<Resource<List<Message>>> = flow {
        emit(Resource.Loading)
        delay(300)
        _messages.collect { allMessages ->
            emit(Resource.Success(allMessages[roomId] ?: emptyList()))
        }
    }

    suspend fun sendMessage(roomId: String, senderId: String, senderName: String, content: String) {
        val newMessage = Message(
            id = UUID.randomUUID().toString(),
            roomId = roomId,
            senderId = senderId,
            senderName = senderName,
            content = content,
            createdAt = Date()
        )
        
        // Update messages
        _messages.update { current ->
            val roomMsgs = current[roomId]?.toMutableList() ?: mutableListOf()
            roomMsgs.add(newMessage)
            current + (roomId to roomMsgs)
        }

        // Update room's last message
        _rooms.update { rooms ->
            rooms.map { room ->
<<<<<<< Updated upstream
                if (room.id == roomId) room.copy(lastMessage = newMessage, unreadCount = emptyMap())
=======
                if (room.id == roomId) room.copy(lastMessage = newMessage, unreadCount = mapOf("me" to 0))
>>>>>>> Stashed changes
                else room
            }
        }
    }

    fun getContacts(): Flow<List<User>> = flow {
        emit(_users)
    }

    fun getCurrentUser() = User("me", "Demo User", "me@demo.com")
}
