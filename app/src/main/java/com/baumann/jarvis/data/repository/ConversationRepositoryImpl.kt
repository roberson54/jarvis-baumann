package com.baumann.jarvis.data.repository

import com.baumann.jarvis.data.local.ConversationDao
import com.baumann.jarvis.data.local.ConversationEntity
import com.baumann.jarvis.domain.model.ConversationMessage
import com.baumann.jarvis.domain.model.Role
import com.baumann.jarvis.domain.repository.ConversationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ConversationRepositoryImpl(
    private val dao: ConversationDao
) : ConversationRepository {

    override suspend fun add(role: Role, content: String) {
        dao.insert(
            ConversationEntity(
                role = role.name,
                content = content,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    override suspend fun recent(limit: Int): List<ConversationMessage> =
        dao.recent(limit).map { it.toModel() }.reversed()

    override fun observeAll(): Flow<List<ConversationMessage>> =
        dao.observeAll().map { list -> list.map { it.toModel() } }

    override suspend fun clear() = dao.clear()

    private fun ConversationEntity.toModel() = ConversationMessage(
        id = id,
        role = runCatching { Role.valueOf(role) }.getOrDefault(Role.SYSTEM),
        content = content,
        timestamp = timestamp
    )
}
