package com.example.worldengine.core.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A timeline event belongs to exactly one world (deleting the world deletes its events). It may
 * optionally reference a character in that world; if that character is deleted the link is cleared
 * (SET_NULL) rather than removing the event.
 */
@Entity(
    tableName = "timeline_events",
    foreignKeys = [
        ForeignKey(
            entity = WorldEntity::class,
            parentColumns = ["id"],
            childColumns = ["worldId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = CharacterEntity::class,
            parentColumns = ["id"],
            childColumns = ["characterId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("worldId"), Index("characterId")],
)
data class TimelineEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val worldId: Long,
    val name: String,
    val dateLabel: String,
    /** Numeric ordering value; lower = earlier. Decouples chronology from the freeform date label. */
    val sortKey: Long,
    val description: String,
    val characterId: Long?,
    val location: String,
    val duration: String,
    val createdAt: Long,
)
