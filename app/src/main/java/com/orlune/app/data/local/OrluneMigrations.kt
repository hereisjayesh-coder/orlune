package com.orlune.app.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Explicit, data-preserving schema migrations for the local Orlune database. */
object OrluneMigrations {
    /**
     * Adds the Phase 6 fields while preserving every existing version-1 row.
     * Both columns are non-null in the v2 entities, so existing rows receive an
     * empty value rather than NULL.
     */
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE schedules ADD COLUMN name TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE focus_sessions ADD COLUMN blockedPackages TEXT NOT NULL DEFAULT ''")
        }
    }
}
