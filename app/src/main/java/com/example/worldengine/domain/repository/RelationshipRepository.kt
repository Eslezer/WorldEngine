package com.example.worldengine.domain.repository

import com.example.worldengine.domain.model.CharacterRelationship
import kotlinx.coroutines.flow.Flow

interface RelationshipRepository {
    fun observeRelationships(worldId: Long): Flow<List<CharacterRelationship>>
    suspend fun save(relationship: CharacterRelationship): Long
    suspend fun delete(relationship: CharacterRelationship)
}
