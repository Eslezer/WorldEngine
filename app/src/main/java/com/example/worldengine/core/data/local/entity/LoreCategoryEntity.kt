package com.example.worldengine.core.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** A customisable world-scoped category for lore/codex entries. */
@Entity(
    tableName = "lore_categories",
    foreignKeys = [
        ForeignKey(
            entity = WorldEntity::class,
            parentColumns = ["id"],
            childColumns = ["worldId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("worldId"), Index(value = ["worldId", "name"])],
)
data class LoreCategoryEntity(
    @PrimaryKey val id: String,
    val worldId: Long,
    val name: String,
    val description: String,
    val colorHex: String,
    val icon: String,
    val isDefault: Boolean,
    val sortOrder: Int,
    val createdAt: Long,
)
