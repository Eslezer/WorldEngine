package com.example.worldengine.data.mapper

import com.example.worldengine.core.data.local.entity.TimelineEventEntity
import com.example.worldengine.domain.model.TimelineEvent

fun TimelineEventEntity.toDomain(): TimelineEvent = TimelineEvent(
    id = id,
    worldId = worldId,
    name = name,
    dateLabel = dateLabel,
    sortKey = sortKey,
    description = description,
    characterId = characterId,
    location = location,
    duration = duration,
    createdAt = createdAt,
)

fun TimelineEvent.toEntity(): TimelineEventEntity = TimelineEventEntity(
    id = id,
    worldId = worldId,
    name = name,
    dateLabel = dateLabel,
    sortKey = sortKey,
    description = description,
    characterId = characterId,
    location = location,
    duration = duration,
    createdAt = if (createdAt == 0L) System.currentTimeMillis() else createdAt,
)
