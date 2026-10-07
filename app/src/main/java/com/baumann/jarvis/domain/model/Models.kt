package com.baumann.jarvis.domain.model

enum class Role { USER, ASSISTANT, SYSTEM, TOOL }

enum class JarvisState { IDLE, LISTENING, PROCESSING, SPEAKING, ERROR }

data class ConversationMessage(
    val id: Long,
    val role: Role,
    val content: String,
    val timestamp: Long
)
