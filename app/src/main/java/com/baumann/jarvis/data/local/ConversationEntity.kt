package com.baumann.jarvis.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Conversation")
data class ConversationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val role: String,
    val content: String,
    val timestamp: Long
)
