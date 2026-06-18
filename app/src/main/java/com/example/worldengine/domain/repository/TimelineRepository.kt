package com.example.worldengine.domain.repository

import com.example.worldengine.domain.model.TimelineEvent
import kotlinx.coroutines.flow.Flow

interface TimelineRepository {
    /** Events for a world, ordered earliest → latest by sort key. */
    fun observeEvents(worldId: Long): Flow<List<TimelineEvent>>
    suspend fun save(event: TimelineEvent): Long
    suspend fun delete(event: TimelineEvent)
}
