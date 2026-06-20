package com.example.worldengine.data.repository

import com.example.worldengine.core.data.local.dao.CharacterRelationshipDao
import com.example.worldengine.data.mapper.toDomain
import com.example.worldengine.data.mapper.toEntity
import com.example.worldengine.domain.model.CharacterRelationship
import com.example.worldengine.domain.repository.RelationshipRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class RelationshipRepositoryImpl(
    private val dao: CharacterRelationshipDao,
    private val ioDispatcher: CoroutineDispatcher,
) : RelationshipRepository {

    override fun observeRelationships(worldId: Long): Flow<List<CharacterRelationship>> =
        dao.observeByWorld(worldId).map { entities -> entities.map { it.toDomain() } }

    override suspend fun save(relationship: CharacterRelationship): Long =
        withContext(ioDispatcher) { dao.upsert(relationship.toEntity()) }

    override suspend fun delete(relationship: CharacterRelationship) =
        withContext(ioDispatcher) { dao.delete(relationship.toEntity()) }
}
