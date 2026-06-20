package com.example.worldengine.core.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.worldengine.core.data.local.dao.CharacterDao
import com.example.worldengine.core.data.local.dao.CharacterRelationshipDao
import com.example.worldengine.core.data.local.dao.TimelineEventDao
import com.example.worldengine.core.data.local.dao.WorldDao
import com.example.worldengine.core.data.local.entity.CharacterEntity
import com.example.worldengine.core.data.local.entity.CharacterRelationshipEntity
import com.example.worldengine.core.data.local.entity.TimelineEventEntity
import com.example.worldengine.core.data.local.entity.WorldEntity

@Database(
    entities = [
        WorldEntity::class,
        CharacterEntity::class,
        TimelineEventEntity::class,
        CharacterRelationshipEntity::class,
    ],
    version = 5,
    exportSchema = true,
)
abstract class WorldEngineDatabase : RoomDatabase() {
    abstract fun worldDao(): WorldDao
    abstract fun characterDao(): CharacterDao
    abstract fun timelineEventDao(): TimelineEventDao
    abstract fun characterRelationshipDao(): CharacterRelationshipDao

    companion object {
        const val NAME = "world_engine.db"
    }
}
