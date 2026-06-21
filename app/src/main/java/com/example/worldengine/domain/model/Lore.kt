package com.example.worldengine.domain.model

/** A world-scoped bucket for reusable codex entries: Places, Factions, Terms, Magic, etc. */
data class LoreCategory(
    val id: String,
    val worldId: Long,
    val name: String,
    val description: String = "",
    val colorHex: String = "#6750A4",
    val icon: String = "Article",
    val isDefault: Boolean = false,
    val sortOrder: Int = 0,
    val createdAt: Long = 0,
)

/**
 * A dictionary/codex entry for the world. These entries are intentionally general so later features
 * can link to them: characters can belong to a Faction entry, maps can pin Place entries, and a
 * story reader can match [title] or [aliases] to show a quick overview.
 */
data class LoreEntry(
    val id: Long = 0,
    val worldId: Long,
    val categoryId: String? = null,
    val title: String,
    val summary: String = "",
    val body: String = "",
    val aliases: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val imagePath: String? = null,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
) {
    fun matches(query: String): Boolean {
        val q = query.trim()
        if (q.isEmpty()) return true
        return title.contains(q, ignoreCase = true) ||
            summary.contains(q, ignoreCase = true) ||
            body.contains(q, ignoreCase = true) ||
            aliases.any { it.contains(q, ignoreCase = true) } ||
            tags.any { it.contains(q, ignoreCase = true) }
    }
}

/** Links a character to a lore entry: faction membership, home place, magic school, culture, etc. */
data class CharacterLoreLink(
    val characterId: Long,
    val loreEntryId: Long,
    val createdAt: Long = 0,
)

/** Links a timeline event to a lore entry: place, faction, artifact, battle, term, etc. */
data class TimelineEventLoreLink(
    val eventId: Long,
    val loreEntryId: Long,
    val createdAt: Long = 0,
)
