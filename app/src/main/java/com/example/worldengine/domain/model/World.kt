package com.example.worldengine.domain.model

/** A creative universe. The root entity that owns characters, maps, timelines, relationships, lore. */
data class World(
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val createdAt: Long = 0,
)
