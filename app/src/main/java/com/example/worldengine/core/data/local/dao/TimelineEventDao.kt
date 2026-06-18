package com.example.worldengine.core.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.example.worldengine.core.data.local.entity.TimelineEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TimelineEventDao {

    /** Events for a world in chronological order (earliest first); the UI can reverse for display. */
    @Query("SELECT * FROM timeline_events WHERE worldId = :worldId ORDER BY sortKey ASC, createdAt ASC")
    fun observeByWorld(worldId: Long): Flow<List<TimelineEventEntity>>

    @Upsert
    suspend fun upsert(event: TimelineEventEntity): Long

    @Delete
    suspend fun delete(event: TimelineEventEntity)
}
