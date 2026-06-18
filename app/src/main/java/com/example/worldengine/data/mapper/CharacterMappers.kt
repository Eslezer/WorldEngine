package com.example.worldengine.data.mapper

import com.example.worldengine.core.data.local.entity.CharacterEntity
import com.example.worldengine.domain.model.Character

fun CharacterEntity.toDomain(): Character = Character(
    id = id,
    worldId = worldId,
    name = name,
    role = role,
    description = description,
    portraitPath = portraitPath,
    createdAt = createdAt,
)

fun Character.toEntity(): CharacterEntity = CharacterEntity(
    id = id,
    worldId = worldId,
    name = name,
    role = role,
    description = description,
    portraitPath = portraitPath,
    createdAt = if (createdAt == 0L) System.currentTimeMillis() else createdAt,
)
