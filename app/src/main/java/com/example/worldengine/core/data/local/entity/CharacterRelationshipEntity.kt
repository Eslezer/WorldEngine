package com.example.worldengine.core.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A directed relationship edge between two characters in the same world: [fromCharacterId] →
 * [toCharacterId]. Direction lets one table express symmetric links (friendships), trees (parent →
 * child) and hierarchies (superior → subordinate). Deleting the world or either endpoint character
 * removes the edge.
 */
@Entity(
    tableName = "character_relationships",
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
            childColumns = ["fromCharacterId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = CharacterEntity::class,
            parentColumns = ["id"],
            childColumns = ["toCharacterId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("worldId"), Index("fromCharacterId"), Index("toCharacterId")],
)
data class CharacterRelationshipEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val worldId: Long,
    val fromCharacterId: Long,
    val toCharacterId: Long,
    /** Stored name of the base [com.example.worldengine.domain.model.RelationshipType] template. */
    val type: String,
    /** Optional free-text label, e.g. "mentor", "estranged sister". */
    val label: String,
    val createdAt: Long,
    /** Id of a [com.example.worldengine.domain.model.CustomRelationshipType], or null for a built-in. */
    val customTypeId: String? = null,
)
