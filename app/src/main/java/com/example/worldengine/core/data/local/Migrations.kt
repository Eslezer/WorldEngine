package com.example.worldengine.core.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Hand-written Room migrations. The SQL is copied verbatim from the exported schema JSON in
 * `app/schemas`, so the post-migration database matches what Room expects exactly. Keeping these
 * means user data (worlds, characters, …) survives schema changes instead of being wiped.
 *
 * Versions ≤ 1 predate exported schemas and are handled by a destructive fallback in the database
 * builder; everything from v2 onward migrates in place.
 */

/** v2 → v3: introduced the timeline_events table (single-point events, no calendar/period columns). */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `timeline_events` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`worldId` INTEGER NOT NULL, `name` TEXT NOT NULL, `dateLabel` TEXT NOT NULL, " +
                "`sortKey` INTEGER NOT NULL, `description` TEXT NOT NULL, `characterId` INTEGER, " +
                "`location` TEXT NOT NULL, `duration` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, " +
                "FOREIGN KEY(`worldId`) REFERENCES `worlds`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , " +
                "FOREIGN KEY(`characterId`) REFERENCES `characters`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL )",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_timeline_events_worldId` ON `timeline_events` (`worldId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_timeline_events_characterId` ON `timeline_events` (`characterId`)")
    }
}

/**
 * v3 → v4: adds custom-calendar + period columns to timeline_events, and the character_relationships
 * table. All new timeline columns are nullable, so plain ALTER ADD COLUMN statements are sufficient.
 */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `timeline_events` ADD COLUMN `calendarId` TEXT")
        db.execSQL("ALTER TABLE `timeline_events` ADD COLUMN `endSortKey` INTEGER")
        db.execSQL("ALTER TABLE `timeline_events` ADD COLUMN `endDateLabel` TEXT")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `character_relationships` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`worldId` INTEGER NOT NULL, `fromCharacterId` INTEGER NOT NULL, " +
                "`toCharacterId` INTEGER NOT NULL, `type` TEXT NOT NULL, `label` TEXT NOT NULL, " +
                "`createdAt` INTEGER NOT NULL, " +
                "FOREIGN KEY(`worldId`) REFERENCES `worlds`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , " +
                "FOREIGN KEY(`fromCharacterId`) REFERENCES `characters`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , " +
                "FOREIGN KEY(`toCharacterId`) REFERENCES `characters`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_character_relationships_worldId` ON `character_relationships` (`worldId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_character_relationships_fromCharacterId` ON `character_relationships` (`fromCharacterId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_character_relationships_toCharacterId` ON `character_relationships` (`toCharacterId`)")
    }
}

/** v4 → v5: adds the optional custom-relationship-type link to character_relationships. */
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `character_relationships` ADD COLUMN `customTypeId` TEXT")
    }
}
