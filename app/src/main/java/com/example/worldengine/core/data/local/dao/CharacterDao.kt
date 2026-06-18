package com.example.worldengine.core.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.example.worldengine.core.data.local.entity.CharacterEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CharacterDao {

    @Query("SELECT * FROM characters WHERE worldId = :worldId ORDER BY createdAt DESC")
    fun observeByWorld(worldId: Long): Flow<List<CharacterEntity>>

    /** Every character across all worlds — used by the Image Generation tab's portrait picker. */
    @Query("SELECT * FROM characters ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<CharacterEntity>>

    @Query("SELECT * FROM characters WHERE id = :id")
    suspend fun getById(id: Long): CharacterEntity?

    /** Targeted update so assigning a portrait doesn't require loading/rewriting the whole row. */
    @Query("UPDATE characters SET portraitPath = :path WHERE id = :id")
    suspend fun updatePortrait(id: Long, path: String)

    @Upsert
    suspend fun upsert(character: CharacterEntity): Long

    @Delete
    suspend fun delete(character: CharacterEntity)
}
