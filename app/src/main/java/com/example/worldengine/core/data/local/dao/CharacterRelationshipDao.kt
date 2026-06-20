package com.example.worldengine.core.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.example.worldengine.core.data.local.entity.CharacterRelationshipEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CharacterRelationshipDao {

    @Query("SELECT * FROM character_relationships WHERE worldId = :worldId ORDER BY createdAt ASC")
    fun observeByWorld(worldId: Long): Flow<List<CharacterRelationshipEntity>>

    @Upsert
    suspend fun upsert(relationship: CharacterRelationshipEntity): Long

    @Delete
    suspend fun delete(relationship: CharacterRelationshipEntity)
}
