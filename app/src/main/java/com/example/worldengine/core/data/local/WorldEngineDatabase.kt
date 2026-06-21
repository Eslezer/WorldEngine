package com.example.worldengine.core.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.worldengine.core.data.local.dao.CharacterDao
import com.example.worldengine.core.data.local.dao.CharacterRelationshipDao
import com.example.worldengine.core.data.local.dao.LoreDao
import com.example.worldengine.core.data.local.dao.TimelineEventDao
import com.example.worldengine.core.data.local.dao.WorldDao
import com.example.worldengine.core.data.local.entity.CharacterEntity
import com.example.worldengine.core.data.local.entity.CharacterLoreLinkEntity
import com.example.worldengine.core.data.local.entity.CharacterRelationshipEntity
import com.example.worldengine.core.data.local.entity.LoreCategoryEntity
import com.example.worldengine.core.data.local.entity.LoreEntryEntity
import com.example.worldengine.core.data.local.entity.TimelineEventEntity
import com.example.worldengine.core.data.local.entity.TimelineEventLoreLinkEntity
import com.example.worldengine.core.data.local.entity.WorldEntity

@Database(
    entities = [
        WorldEntity::class,
        CharacterEntity::class,
        TimelineEventEntity::class,
        CharacterRelationshipEntity::class,
        LoreCategoryEntity::class,
        LoreEntryEntity::class,
        CharacterLoreLinkEntity::class,
        TimelineEventLoreLinkEntity::class,
    ],
    version = 8,
    exportSchema = true,
)
abstract class WorldEngineDatabase : RoomDatabase() {
    abstract fun worldDao(): WorldDao
    abstract fun characterDao(): CharacterDao
    abstract fun timelineEventDao(): TimelineEventDao
    abstract fun characterRelationshipDao(): CharacterRelationshipDao
    abstract fun loreDao(): LoreDao

    companion object {
        const val NAME = "world_engine.db"
    }
}
