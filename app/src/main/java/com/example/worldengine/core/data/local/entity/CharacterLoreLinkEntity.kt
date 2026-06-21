package com.example.worldengine.core.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/** Join table connecting characters to lore entries such as factions, places and terms. */
@Entity(
    tableName = "character_lore_links",
    primaryKeys = ["characterId", "loreEntryId"],
    foreignKeys = [
        ForeignKey(
            entity = CharacterEntity::class,
            parentColumns = ["id"],
            childColumns = ["characterId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = LoreEntryEntity::class,
            parentColumns = ["id"],
            childColumns = ["loreEntryId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("characterId"), Index("loreEntryId")],
)
data class CharacterLoreLinkEntity(
    val characterId: Long,
    val loreEntryId: Long,
    val createdAt: Long,
)
