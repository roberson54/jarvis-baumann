package com.baumann.jarvis.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {
    @Insert
    suspend fun insert(entity: ConversationEntity): Long

    @Query("SELECT * FROM Conversation ORDER BY timestamp DESC, id DESC LIMIT :limit")
    suspend fun recent(limit: Int): List<ConversationEntity>

    @Query("SELECT * FROM Conversation ORDER BY timestamp DESC, id DESC")
    fun observeAll(): Flow<List<ConversationEntity>>

    @Query("DELETE FROM Conversation")
    suspend fun clear()
}
