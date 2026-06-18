package com.example.worldengine.data.repository

import com.example.worldengine.core.data.local.dao.WorldDao
import com.example.worldengine.data.mapper.toDomain
import com.example.worldengine.data.mapper.toEntity
import com.example.worldengine.domain.model.World
import com.example.worldengine.domain.repository.WorldRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class WorldRepositoryImpl(
    private val dao: WorldDao,
    private val ioDispatcher: CoroutineDispatcher,
) : WorldRepository {

    override fun observeWorlds(): Flow<List<World>> =
        dao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override suspend fun getWorld(id: Long): World? =
        withContext(ioDispatcher) { dao.getById(id)?.toDomain() }

    override suspend fun save(world: World): Long =
        withContext(ioDispatcher) { dao.upsert(world.toEntity()) }

    override suspend fun delete(world: World) =
        withContext(ioDispatcher) { dao.delete(world.toEntity()) }
}
