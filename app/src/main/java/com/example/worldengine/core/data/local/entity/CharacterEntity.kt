package com.example.worldengine.core.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** A character belongs to exactly one world; deleting the world deletes its characters. */
@Entity(
    tableName = "characters",
    foreignKeys = [
        ForeignKey(
            entity = WorldEntity::class,
            parentColumns = ["id"],
            childColumns = ["worldId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("worldId")],
)
data class CharacterEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val worldId: Long,
    val name: String,
    val role: String,
    val description: String,
    /** Absolute path to a locally-stored portrait image (e.g. a NovelAI generation), or null. */
    val portraitPath: String?,
    val createdAt: Long,
)
