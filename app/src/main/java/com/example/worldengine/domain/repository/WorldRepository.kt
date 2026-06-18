package com.example.worldengine.domain.repository

import com.example.worldengine.domain.model.World
import kotlinx.coroutines.flow.Flow

interface WorldRepository {
    fun observeWorlds(): Flow<List<World>>
    suspend fun getWorld(id: Long): World?
    suspend fun save(world: World): Long
    suspend fun delete(world: World)
}
