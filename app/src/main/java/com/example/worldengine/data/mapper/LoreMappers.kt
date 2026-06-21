package com.example.worldengine.data.mapper

import com.example.worldengine.core.data.local.entity.LoreCategoryEntity
import com.example.worldengine.core.data.local.entity.CharacterLoreLinkEntity
import com.example.worldengine.core.data.local.entity.LoreEntryEntity
import com.example.worldengine.core.data.local.entity.TimelineEventLoreLinkEntity
import com.example.worldengine.domain.model.CharacterLoreLink
import com.example.worldengine.domain.model.LoreCategory
import com.example.worldengine.domain.model.LoreEntry
import com.example.worldengine.domain.model.TimelineEventLoreLink

fun LoreCategoryEntity.toDomain(): LoreCategory = LoreCategory(
    id = id,
    worldId = worldId,
    name = name,
    description = description,
    colorHex = colorHex,
    icon = icon,
    isDefault = isDefault,
    sortOrder = sortOrder,
    createdAt = createdAt,
)

fun LoreCategory.toEntity(): LoreCategoryEntity = LoreCategoryEntity(
    id = id,
    worldId = worldId,
    name = name,
    description = description,
    colorHex = colorHex,
    icon = icon,
    isDefault = isDefault,
    sortOrder = sortOrder,
    createdAt = if (createdAt == 0L) System.currentTimeMillis() else createdAt,
)

fun LoreEntryEntity.toDomain(): LoreEntry = LoreEntry(
    id = id,
    worldId = worldId,
    categoryId = categoryId,
    title = title,
    summary = summary,
    body = body,
    aliases = aliases.toLineList(),
    tags = tags.toLineList(),
    imagePath = imagePath,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun LoreEntry.toEntity(): LoreEntryEntity {
    val now = System.currentTimeMillis()
    return LoreEntryEntity(
        id = id,
        worldId = worldId,
        categoryId = categoryId,
        title = title,
        summary = summary,
        body = body,
        aliases = aliases.toStorageString(),
        tags = tags.toStorageString(),
        imagePath = imagePath,
        createdAt = if (createdAt == 0L) now else createdAt,
        updatedAt = now,
    )
}

private fun String.toLineList(): List<String> =
    lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.toList()

private fun List<String>.toStorageString(): String =
    map { it.trim() }.filter { it.isNotEmpty() }.joinToString("\n")

fun CharacterLoreLinkEntity.toDomain(): CharacterLoreLink = CharacterLoreLink(
    characterId = characterId,
    loreEntryId = loreEntryId,
    createdAt = createdAt,
)

fun CharacterLoreLink.toEntity(): CharacterLoreLinkEntity = CharacterLoreLinkEntity(
    characterId = characterId,
    loreEntryId = loreEntryId,
    createdAt = if (createdAt == 0L) System.currentTimeMillis() else createdAt,
)

fun TimelineEventLoreLinkEntity.toDomain(): TimelineEventLoreLink = TimelineEventLoreLink(
    eventId = eventId,
    loreEntryId = loreEntryId,
    createdAt = createdAt,
)

fun TimelineEventLoreLink.toEntity(): TimelineEventLoreLinkEntity = TimelineEventLoreLinkEntity(
    eventId = eventId,
    loreEntryId = loreEntryId,
    createdAt = if (createdAt == 0L) System.currentTimeMillis() else createdAt,
)
