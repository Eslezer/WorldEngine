package com.example.worldengine.core.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.example.worldengine.core.data.local.entity.WorldEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorldDao {

    @Query("SELECT * FROM worlds ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<WorldEntity>>

    @Query("SELECT * FROM worlds WHERE id = :id")
    suspend fun getById(id: Long): WorldEntity?

    /** Inserts a new world or updates an existing one (matched by primary key). Returns the row id. */
    @Upsert
    suspend fun upsert(world: WorldEntity): Long

    @Delete
    suspend fun delete(world: WorldEntity)
}
