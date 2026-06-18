package com.example.worldengine.core.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.worldengine.core.data.local.dao.CharacterDao
import com.example.worldengine.core.data.local.dao.WorldDao
import com.example.worldengine.core.data.local.entity.CharacterEntity
import com.example.worldengine.core.data.local.entity.WorldEntity

@Database(
    entities = [WorldEntity::class, CharacterEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class WorldEngineDatabase : RoomDatabase() {
    abstract fun worldDao(): WorldDao
    abstract fun characterDao(): CharacterDao

    companion object {
        const val NAME = "world_engine.db"
    }
}
