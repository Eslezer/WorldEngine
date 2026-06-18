package com.example.worldengine.data.repository

import com.example.worldengine.core.data.local.dao.TimelineEventDao
import com.example.worldengine.data.mapper.toDomain
import com.example.worldengine.data.mapper.toEntity
import com.example.worldengine.domain.model.TimelineEvent
import com.example.worldengine.domain.repository.TimelineRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class TimelineRepositoryImpl(
    private val dao: TimelineEventDao,
    private val ioDispatcher: CoroutineDispatcher,
) : TimelineRepository {

    override fun observeEvents(worldId: Long): Flow<List<TimelineEvent>> =
        dao.observeByWorld(worldId).map { entities -> entities.map { it.toDomain() } }

    override suspend fun save(event: TimelineEvent): Long =
        withContext(ioDispatcher) { dao.upsert(event.toEntity()) }

    override suspend fun delete(event: TimelineEvent) =
        withContext(ioDispatcher) { dao.delete(event.toEntity()) }
}
