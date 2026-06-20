package com.example.worldengine.data.mapper

import com.example.worldengine.core.data.local.entity.CharacterRelationshipEntity
import com.example.worldengine.domain.model.CharacterRelationship
import com.example.worldengine.domain.model.RelationshipType

fun CharacterRelationshipEntity.toDomain(): CharacterRelationship = CharacterRelationship(
    id = id,
    worldId = worldId,
    fromCharacterId = fromCharacterId,
    toCharacterId = toCharacterId,
    type = runCatching { RelationshipType.valueOf(type) }.getOrDefault(RelationshipType.FRIEND),
    label = label,
    createdAt = createdAt,
    customTypeId = customTypeId,
)

fun CharacterRelationship.toEntity(): CharacterRelationshipEntity = CharacterRelationshipEntity(
    id = id,
    worldId = worldId,
    fromCharacterId = fromCharacterId,
    toCharacterId = toCharacterId,
    type = type.name,
    label = label,
    createdAt = if (createdAt == 0L) System.currentTimeMillis() else createdAt,
    customTypeId = customTypeId,
)
