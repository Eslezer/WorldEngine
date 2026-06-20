package com.example.worldengine.ui.navigation

/**
 * Navigation routes. Top-level routes are reachable from the bottom navigation bar; the Worlds
 * subtree drills down World list → World detail → Character editor.
 */
object Routes {
    const val IMAGE_GENERATION = "imagegen"
    const val WORLDS = "worlds"
    const val SETTINGS = "settings"

    /** Global managers reached from Settings (not top-level bottom-bar sections). */
    const val CALENDARS = "calendars"
    const val RELATIONSHIP_TYPES = "relationship_types"

    const val WORLD_DETAIL = "world/{worldId}"
    const val CHARACTER_EDITOR = "world/{worldId}/character/{characterId}"

    const val ARG_WORLD_ID = "worldId"
    const val ARG_CHARACTER_ID = "characterId"

    /** 0 means "create a new character". */
    fun worldDetail(worldId: Long) = "world/$worldId"
    fun characterEditor(worldId: Long, characterId: Long = 0) = "world/$worldId/character/$characterId"
}
