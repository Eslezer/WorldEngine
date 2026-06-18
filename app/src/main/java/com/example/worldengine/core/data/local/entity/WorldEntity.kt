package com.example.worldengine.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A world is the top-level container of the app: characters, maps, timelines, relationships and
 * lore all belong to a world. Those child tables (with foreign keys to this one) are added as
 * their features are built.
 */
@Entity(tableName = "worlds")
data class WorldEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String,
    val createdAt: Long,
)
