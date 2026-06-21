package com.example.worldengine.core.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** A reusable codex/dictionary entry that belongs to a world and optionally to a category. */
@Entity(
    tableName = "lore_entries",
    foreignKeys = [
        ForeignKey(
            entity = WorldEntity::class,
            parentColumns = ["id"],
            childColumns = ["worldId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = LoreCategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("worldId"), Index("categoryId"), Index(value = ["worldId", "title"])],
)
data class LoreEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val worldId: Long,
    val categoryId: String?,
    val title: String,
    val summary: String,
    val body: String,
    /** Newline-separated aliases. Stored as text to avoid a full converter layer for one table. */
    val aliases: String,
    /** Newline-separated tags. */
    val tags: String,
    val imagePath: String?,
    val createdAt: Long,
    val updatedAt: Long,
)
