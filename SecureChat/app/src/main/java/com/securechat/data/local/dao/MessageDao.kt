package com.securechat.data.local.dao

import androidx.room.*
import com.securechat.data.local.entity.MessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {

    @Query("SELECT * FROM messages WHERE roomId = :roomId ORDER BY createdAt ASC")
    fun getMessagesByRoom(roomId: String): Flow<List<MessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<MessageEntity>)

    @Query("DELETE FROM messages WHERE roomId = :roomId")
    suspend fun deleteRoomMessages(roomId: String)

    @Query("SELECT COUNT(*) FROM messages WHERE roomId = :roomId AND isRead = 0 AND senderId != :currentUserId")
    fun getUnreadCount(roomId: String, currentUserId: String): Flow<Int>
}
