package com.example.worldengine.core.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/** Join table connecting timeline events to lore/codex entries. */
@Entity(
    tableName = "timeline_event_lore_links",
    primaryKeys = ["eventId", "loreEntryId"],
    foreignKeys = [
        ForeignKey(
            entity = TimelineEventEntity::class,
            parentColumns = ["id"],
            childColumns = ["eventId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = LoreEntryEntity::class,
            parentColumns = ["id"],
            childColumns = ["loreEntryId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("eventId"), Index("loreEntryId")],
)
data class TimelineEventLoreLinkEntity(
    val eventId: Long,
    val loreEntryId: Long,
    val createdAt: Long,
)
