package com.baumann.jarvis.domain.repository

import com.baumann.jarvis.domain.model.ConversationMessage
import com.baumann.jarvis.domain.model.Role
import kotlinx.coroutines.flow.Flow

interface ConversationRepository {
    suspend fun add(role: Role, content: String)

    /** Últimas [limit] mensagens, em ordem cronológica (mais antiga primeiro). */
    suspend fun recent(limit: Int): List<ConversationMessage>

    /** Todas as mensagens, da mais nova para a mais antiga. */
    fun observeAll(): Flow<List<ConversationMessage>>

    suspend fun clear()
}
