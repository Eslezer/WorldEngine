package com.example.worldengine.domain.model

/** A character that belongs to a [World]. */
data class Character(
    val id: Long = 0,
    val worldId: Long,
    val name: String,
    val role: String = "",
    val description: String = "",
    val portraitPath: String? = null,
    val createdAt: Long = 0,
)
