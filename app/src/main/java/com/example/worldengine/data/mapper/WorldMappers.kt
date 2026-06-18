package com.example.worldengine.data.mapper

import com.example.worldengine.core.data.local.entity.WorldEntity
import com.example.worldengine.domain.model.World

fun WorldEntity.toDomain(): World = World(
    id = id,
    name = name,
    description = description,
    createdAt = createdAt,
)

fun World.toEntity(): WorldEntity = WorldEntity(
    id = id,
    name = name,
    description = description,
    createdAt = if (createdAt == 0L) System.currentTimeMillis() else createdAt,
)
