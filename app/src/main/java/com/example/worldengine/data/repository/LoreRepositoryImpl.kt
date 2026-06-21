package com.example.worldengine.data.repository

import com.example.worldengine.core.data.local.dao.LoreDao
import com.example.worldengine.data.mapper.toDomain
import com.example.worldengine.data.mapper.toEntity
import com.example.worldengine.domain.model.CharacterLoreLink
import com.example.worldengine.domain.model.LoreCategory
import com.example.worldengine.domain.model.LoreEntry
import com.example.worldengine.domain.model.TimelineEventLoreLink
import com.example.worldengine.domain.repository.LoreRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class LoreRepositoryImpl(
    private val dao: LoreDao,
    private val ioDispatcher: CoroutineDispatcher,
) : LoreRepository {

    override fun observeCategories(worldId: Long): Flow<List<LoreCategory>> =
        dao.observeCategories(worldId).map { entities -> entities.map { it.toDomain() } }

    override fun observeEntries(worldId: Long): Flow<List<LoreEntry>> =
        dao.observeEntries(worldId).map { entities -> entities.map { it.toDomain() } }

    override fun observeCharacterLinks(worldId: Long): Flow<List<CharacterLoreLink>> =
        dao.observeCharacterLinks(worldId).map { entities -> entities.map { it.toDomain() } }

    override fun observeTimelineLinks(worldId: Long): Flow<List<TimelineEventLoreLink>> =
        dao.observeTimelineLinks(worldId).map { entities -> entities.map { it.toDomain() } }

    override suspend fun ensureDefaultCategories(worldId: Long) = withContext(ioDispatcher) {
        if (dao.categoryCount(worldId) > 0) return@withContext
        val now = System.currentTimeMillis()
        dao.insertCategories(defaultCategories(worldId, now).map { it.toEntity() })
    }

    override suspend fun getLinksForCharacter(characterId: Long): List<CharacterLoreLink> =
        withContext(ioDispatcher) { dao.getLinksForCharacter(characterId).map { it.toDomain() } }

    override suspend fun setCharacterLinks(characterId: Long, loreEntryIds: Set<Long>) =
        withContext(ioDispatcher) {
            dao.replaceCharacterLinks(
                characterId = characterId,
                links = loreEntryIds.map { CharacterLoreLink(characterId, it).toEntity() },
            )
        }

    override suspend fun getLinksForEvent(eventId: Long): List<TimelineEventLoreLink> =
        withContext(ioDispatcher) { dao.getLinksForEvent(eventId).map { it.toDomain() } }

    override suspend fun setTimelineLinks(eventId: Long, loreEntryIds: Set<Long>) =
        withContext(ioDispatcher) {
            dao.replaceTimelineLinks(
                eventId = eventId,
                links = loreEntryIds.map { TimelineEventLoreLink(eventId, it).toEntity() },
            )
        }

    override suspend fun saveCategory(category: LoreCategory) =
        withContext(ioDispatcher) { dao.upsertCategory(category.toEntity()) }

    override suspend fun deleteCategory(category: LoreCategory) =
        withContext(ioDispatcher) { dao.deleteCategory(category.toEntity()) }

    override suspend fun saveEntry(entry: LoreEntry): Long =
        withContext(ioDispatcher) { dao.upsertEntry(entry.toEntity()) }

    override suspend fun deleteEntry(entry: LoreEntry) =
        withContext(ioDispatcher) { dao.deleteEntry(entry.toEntity()) }

    private fun defaultCategories(worldId: Long, createdAt: Long): List<LoreCategory> = listOf(
        LoreCategory("default-$worldId-places", worldId, "Places", "Locations, regions, cities and landmarks.", "#2E7D32", "Place", true, 0, createdAt),
        LoreCategory("default-$worldId-factions", worldId, "Factions", "Nations, guilds, houses, clans and organizations.", "#1565C0", "Groups", true, 1, createdAt),
        LoreCategory("default-$worldId-cultures", worldId, "Cultures", "Customs, peoples, etiquette and social patterns.", "#6D4C41", "Culture", true, 2, createdAt),
        LoreCategory("default-$worldId-magic-tech", worldId, "Magic / Technology", "Power systems, science, devices and rules.", "#7B1FA2", "AutoAwesome", true, 3, createdAt),
        LoreCategory("default-$worldId-artifacts", worldId, "Artifacts", "Objects, relics, weapons and important materials.", "#EF6C00", "Diamond", true, 4, createdAt),
        LoreCategory("default-$worldId-languages", worldId, "Languages", "Names, scripts, sayings and linguistic notes.", "#00838F", "Translate", true, 5, createdAt),
        LoreCategory("default-$worldId-beliefs", worldId, "Beliefs", "Religions, philosophies, myths and taboos.", "#AD1457", "Temple", true, 6, createdAt),
        LoreCategory("default-$worldId-terms", worldId, "Terms", "Glossary words, concepts and recurring references.", "#455A64", "Dictionary", true, 7, createdAt),
        LoreCategory("default-$worldId-history", worldId, "History Notes", "Era notes, legends and background events.", "#5D4037", "History", true, 8, createdAt),
    )
}
