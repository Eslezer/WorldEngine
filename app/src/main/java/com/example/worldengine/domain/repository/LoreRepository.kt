package com.example.worldengine.domain.repository

import com.example.worldengine.domain.model.CharacterLoreLink
import com.example.worldengine.domain.model.LoreCategory
import com.example.worldengine.domain.model.LoreEntry
import com.example.worldengine.domain.model.TimelineEventLoreLink
import kotlinx.coroutines.flow.Flow

interface LoreRepository {
    fun observeCategories(worldId: Long): Flow<List<LoreCategory>>
    fun observeEntries(worldId: Long): Flow<List<LoreEntry>>
    fun observeCharacterLinks(worldId: Long): Flow<List<CharacterLoreLink>>
    fun observeTimelineLinks(worldId: Long): Flow<List<TimelineEventLoreLink>>
    suspend fun ensureDefaultCategories(worldId: Long)
    suspend fun getLinksForCharacter(characterId: Long): List<CharacterLoreLink>
    suspend fun setCharacterLinks(characterId: Long, loreEntryIds: Set<Long>)
    suspend fun getLinksForEvent(eventId: Long): List<TimelineEventLoreLink>
    suspend fun setTimelineLinks(eventId: Long, loreEntryIds: Set<Long>)
    suspend fun saveCategory(category: LoreCategory)
    suspend fun deleteCategory(category: LoreCategory)
    suspend fun saveEntry(entry: LoreEntry): Long
    suspend fun deleteEntry(entry: LoreEntry)
}
