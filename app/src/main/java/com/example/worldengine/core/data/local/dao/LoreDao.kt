package com.example.worldengine.core.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.example.worldengine.core.data.local.entity.CharacterLoreLinkEntity
import com.example.worldengine.core.data.local.entity.LoreCategoryEntity
import com.example.worldengine.core.data.local.entity.LoreEntryEntity
import com.example.worldengine.core.data.local.entity.TimelineEventLoreLinkEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LoreDao {

    @Query("SELECT * FROM lore_categories WHERE worldId = :worldId ORDER BY sortOrder, name COLLATE NOCASE")
    fun observeCategories(worldId: Long): Flow<List<LoreCategoryEntity>>

    @Query("SELECT * FROM lore_entries WHERE worldId = :worldId ORDER BY updatedAt DESC, title COLLATE NOCASE")
    fun observeEntries(worldId: Long): Flow<List<LoreEntryEntity>>

    @Query(
        "SELECT character_lore_links.* FROM character_lore_links " +
            "INNER JOIN lore_entries ON lore_entries.id = character_lore_links.loreEntryId " +
            "WHERE lore_entries.worldId = :worldId",
    )
    fun observeCharacterLinks(worldId: Long): Flow<List<CharacterLoreLinkEntity>>

    @Query("SELECT * FROM character_lore_links WHERE characterId = :characterId")
    suspend fun getLinksForCharacter(characterId: Long): List<CharacterLoreLinkEntity>

    @Query(
        "SELECT timeline_event_lore_links.* FROM timeline_event_lore_links " +
            "INNER JOIN timeline_events ON timeline_events.id = timeline_event_lore_links.eventId " +
            "WHERE timeline_events.worldId = :worldId",
    )
    fun observeTimelineLinks(worldId: Long): Flow<List<TimelineEventLoreLinkEntity>>

    @Query("SELECT * FROM timeline_event_lore_links WHERE eventId = :eventId")
    suspend fun getLinksForEvent(eventId: Long): List<TimelineEventLoreLinkEntity>

    @Query("SELECT COUNT(*) FROM lore_categories WHERE worldId = :worldId")
    suspend fun categoryCount(worldId: Long): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCategories(categories: List<LoreCategoryEntity>)

    @Upsert
    suspend fun upsertCategory(category: LoreCategoryEntity)

    @Upsert
    suspend fun upsertEntry(entry: LoreEntryEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCharacterLinks(links: List<CharacterLoreLinkEntity>)

    @Query("DELETE FROM character_lore_links WHERE characterId = :characterId")
    suspend fun deleteLinksForCharacter(characterId: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTimelineLinks(links: List<TimelineEventLoreLinkEntity>)

    @Query("DELETE FROM timeline_event_lore_links WHERE eventId = :eventId")
    suspend fun deleteLinksForEvent(eventId: Long)

    @Transaction
    suspend fun replaceCharacterLinks(characterId: Long, links: List<CharacterLoreLinkEntity>) {
        deleteLinksForCharacter(characterId)
        if (links.isNotEmpty()) insertCharacterLinks(links)
    }

    @Transaction
    suspend fun replaceTimelineLinks(eventId: Long, links: List<TimelineEventLoreLinkEntity>) {
        deleteLinksForEvent(eventId)
        if (links.isNotEmpty()) insertTimelineLinks(links)
    }

    @Delete
    suspend fun deleteCategory(category: LoreCategoryEntity)

    @Delete
    suspend fun deleteEntry(entry: LoreEntryEntity)
}
