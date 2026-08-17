package com.orlune.app.data.local

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Verifies that the real version-1 and version-2 schemas upgrade without losing local data. */
@RunWith(AndroidJUnit4::class)
class OrluneDatabaseMigrationTest {

    private var migratedDatabase: OrluneDatabase? = null

    @After
    fun tearDown() {
        migratedDatabase?.close()
        ApplicationProvider.getApplicationContext<Context>().deleteDatabase(DATABASE_NAME)
    }

    @Test
    fun migrate2To3_preservesExistingFocusSessionAndUsesAllowAllDefault() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val version2Helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(DATABASE_NAME)
                .callback(Version2Callback())
                .build()
        )
        version2Helper.writableDatabase.apply {
            execSQL(
                "INSERT INTO focus_sessions (id, startTs, endTs, plannedMinutes, completedMinutes, blockedCategoryIds, blockedPackages) VALUES (?, ?, ?, ?, ?, ?, ?)",
                arrayOf<Any?>(1L, 2_000L, null, 25, 0, "", "com.example.social")
            )
            close()
        }
        version2Helper.close()

        migratedDatabase = Room.databaseBuilder(context, OrluneDatabase::class.java, DATABASE_NAME)
            .addMigrations(OrluneMigrations.MIGRATION_2_3)
            .build()
        val migrated = migratedDatabase!!.openHelper.writableDatabase

        assertEquals(1L, countRows(migrated, "focus_sessions"))
        migrated.query(
            "SELECT startTs, plannedMinutes, blockedPackages, notificationPolicy, allowedNotificationPackages FROM focus_sessions"
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(2_000L, cursor.getLong(0))
            assertEquals(25, cursor.getInt(1))
            assertEquals("com.example.social", cursor.getString(2))
            assertEquals("ALLOW_ALL", cursor.getString(3))
            assertEquals("", cursor.getString(4))
        }

        migrated.close()
    }

    @Test
    fun migrate1To2_preservesExistingRowsAndUsesEmptyDefaults() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val version1Helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(DATABASE_NAME)
                .callback(Version1Callback())
                .build()
        )
        version1Helper.writableDatabase.apply {
            insertVersion1Data(this)
            close()
        }
        version1Helper.close()

        migratedDatabase = Room.databaseBuilder(context, OrluneDatabase::class.java, DATABASE_NAME)
            .addMigrations(OrluneMigrations.MIGRATION_1_2, OrluneMigrations.MIGRATION_2_3)
            .build()
        val migrated = migratedDatabase!!.openHelper.writableDatabase

        assertEquals(1L, countRows(migrated, "apps"))
        assertEquals(1L, countRows(migrated, "app_categories"))
        assertEquals(1L, countRows(migrated, "daily_usage"))
        assertEquals(1L, countRows(migrated, "sessions"))
        assertEquals(1L, countRows(migrated, "focus_sessions"))
        assertEquals(1L, countRows(migrated, "rules"))
        assertEquals(1L, countRows(migrated, "schedules"))
        assertEquals(1L, countRows(migrated, "app_list_entries"))
        assertEquals(1L, countRows(migrated, "goals"))
        assertEquals(1L, countRows(migrated, "habit_records"))
        assertEquals(1L, countRows(migrated, "notification_preferences"))
        assertEquals(1L, countRows(migrated, "theme_preferences"))
        assertEquals(1L, countRows(migrated, "user_preferences"))
        assertEquals(1L, countRows(migrated, "local_recommendations"))
        assertEquals(1L, countRows(migrated, "emergency_overrides"))
        assertEquals(1L, countRows(migrated, "privacy_settings"))

        migrated.query("SELECT packageName, label, category, isEssential FROM apps").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("com.example.social", cursor.getString(0))
            assertEquals("Social", cursor.getString(1))
            assertTrue(cursor.isNull(2))
            assertEquals(0, cursor.getInt(3))
        }

        migrated.query("SELECT packageName, epochDay, totalUsageSeconds, launchCount, sessionCount FROM daily_usage").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("com.example.social", cursor.getString(0))
            assertEquals(20_000L, cursor.getLong(1))
            assertEquals(1_800L, cursor.getLong(2))
            assertEquals(3, cursor.getInt(3))
            assertEquals(2, cursor.getInt(4))
        }

        migrated.query("SELECT packageName, startTs, endTs FROM sessions").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("com.example.social", cursor.getString(0))
            assertEquals(1_000L, cursor.getLong(1))
            assertTrue(cursor.isNull(2))
        }

        migrated.query("SELECT startTs, endTs, plannedMinutes, completedMinutes, blockedCategoryIds, blockedPackages FROM focus_sessions").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(2_000L, cursor.getLong(0))
            assertTrue(cursor.isNull(1))
            assertEquals(25, cursor.getInt(2))
            assertEquals(0, cursor.getInt(3))
            assertEquals("", cursor.getString(4))
            assertEquals("", cursor.getString(5))
        }

        migrated.query("SELECT id, name, daysOfWeek, startTime, endTime, associatedRuleId FROM schedules").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(1L, cursor.getLong(0))
            assertEquals("", cursor.getString(1))
            assertEquals("MON,WED", cursor.getString(2))
            assertEquals("09:00", cursor.getString(3))
            assertEquals("17:00", cursor.getString(4))
            assertEquals(1L, cursor.getLong(5))
        }

        migrated.query("SELECT type, targetPackageOrCategory, threshold, windowDefinition FROM rules").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("schedule", cursor.getString(0))
            assertEquals("com.example.social", cursor.getString(1))
            assertTrue(cursor.isNull(2))
            assertTrue(cursor.isNull(3))
        }

        migrated.query("SELECT packageName, listType FROM app_list_entries").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("com.example.social", cursor.getString(0))
            assertEquals("allow", cursor.getString(1))
        }

        migrated.query("SELECT id, type, targetValue, period FROM goals").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(1L, cursor.getLong(0))
            assertEquals("focus", cursor.getString(1))
            assertEquals(25L, cursor.getLong(2))
            assertEquals("daily", cursor.getString(3))
        }

        migrated.query("SELECT id, epochDay, goalId, met FROM habit_records").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(1L, cursor.getLong(0))
            assertEquals(20_000L, cursor.getLong(1))
            assertEquals(1L, cursor.getLong(2))
            assertEquals(1, cursor.getInt(3))
        }

        migrated.query("SELECT id, quietWindows, digestEnabled FROM notification_preferences").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(1L, cursor.getLong(0))
            assertEquals("", cursor.getString(1))
            assertEquals(0, cursor.getInt(2))
        }

        migrated.query("SELECT id, themeId FROM theme_preferences").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(1L, cursor.getLong(0))
            assertEquals("dark", cursor.getString(1))
        }

        migrated.query("SELECT key, value FROM user_preferences").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("usage.lastProcessedEventTime", cursor.getString(0))
            assertEquals("1000", cursor.getString(1))
        }

        migrated.query("SELECT id, epochDay, ruleSuggestion, basis FROM local_recommendations").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(1L, cursor.getLong(0))
            assertEquals(20_000L, cursor.getLong(1))
            assertEquals("keep social under 30 minutes", cursor.getString(2))
            assertEquals("daily usage", cursor.getString(3))
        }

        migrated.query("SELECT id, timestamp, ruleId, reason FROM emergency_overrides").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(1L, cursor.getLong(0))
            assertEquals(3_000L, cursor.getLong(1))
            assertEquals(1L, cursor.getLong(2))
            assertTrue(cursor.isNull(3))
        }

        migrated.query("SELECT permissionName, granted, lastChecked FROM privacy_settings").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("PACKAGE_USAGE_STATS", cursor.getString(0))
            assertEquals(1, cursor.getInt(1))
            assertEquals(4_000L, cursor.getLong(2))
        }

        migrated.close()
    }

    private fun insertVersion1Data(database: SupportSQLiteDatabase) {
        database.execSQL(
            "INSERT INTO apps (packageName, label, category, isEssential) VALUES (?, ?, ?, ?)",
            arrayOf<Any?>("com.example.social", "Social", null, 0)
        )
        database.execSQL(
            "INSERT INTO app_categories (id, name, isSystemDefined) VALUES (?, ?, ?)",
            arrayOf<Any?>(1L, "Social", 0)
        )
        database.execSQL(
            "INSERT INTO daily_usage (id, packageName, epochDay, totalUsageSeconds, launchCount, sessionCount) VALUES (?, ?, ?, ?, ?, ?)",
            arrayOf<Any?>(1L, "com.example.social", 20_000L, 1_800L, 3, 2)
        )
        database.execSQL(
            "INSERT INTO sessions (id, packageName, startTs, endTs) VALUES (?, ?, ?, ?)",
            arrayOf<Any?>(1L, "com.example.social", 1_000L, null)
        )
        database.execSQL(
            "INSERT INTO focus_sessions (id, startTs, endTs, plannedMinutes, completedMinutes, blockedCategoryIds) VALUES (?, ?, ?, ?, ?, ?)",
            arrayOf<Any?>(1L, 2_000L, null, 25, 0, "")
        )
        database.execSQL(
            "INSERT INTO rules (id, type, targetPackageOrCategory, threshold, windowDefinition) VALUES (?, ?, ?, ?, ?)",
            arrayOf<Any?>(1L, "schedule", "com.example.social", null, null)
        )
        database.execSQL(
            "INSERT INTO schedules (id, daysOfWeek, startTime, endTime, associatedRuleId) VALUES (?, ?, ?, ?, ?)",
            arrayOf<Any?>(1L, "MON,WED", "09:00", "17:00", 1L)
        )
        database.execSQL(
            "INSERT INTO app_list_entries (packageName, listType) VALUES (?, ?)",
            arrayOf<Any?>("com.example.social", "allow")
        )
        database.execSQL(
            "INSERT INTO goals (id, type, targetValue, period) VALUES (?, ?, ?, ?)",
            arrayOf<Any?>(1L, "focus", 25L, "daily")
        )
        database.execSQL(
            "INSERT INTO habit_records (id, epochDay, goalId, met) VALUES (?, ?, ?, ?)",
            arrayOf<Any?>(1L, 20_000L, 1L, 1)
        )
        database.execSQL(
            "INSERT INTO notification_preferences (id, quietWindows, digestEnabled) VALUES (?, ?, ?)",
            arrayOf<Any?>(1L, "", 0)
        )
        database.execSQL(
            "INSERT INTO theme_preferences (id, themeId) VALUES (?, ?)",
            arrayOf<Any?>(1L, "dark")
        )
        database.execSQL(
            "INSERT INTO user_preferences (key, value) VALUES (?, ?)",
            arrayOf<Any?>("usage.lastProcessedEventTime", "1000")
        )
        database.execSQL(
            "INSERT INTO local_recommendations (id, epochDay, ruleSuggestion, basis) VALUES (?, ?, ?, ?)",
            arrayOf<Any?>(1L, 20_000L, "keep social under 30 minutes", "daily usage")
        )
        database.execSQL(
            "INSERT INTO emergency_overrides (id, timestamp, ruleId, reason) VALUES (?, ?, ?, ?)",
            arrayOf<Any?>(1L, 3_000L, 1L, null)
        )
        database.execSQL(
            "INSERT INTO privacy_settings (permissionName, granted, lastChecked) VALUES (?, ?, ?)",
            arrayOf<Any?>("PACKAGE_USAGE_STATS", 1, 4_000L)
        )
    }

    private fun countRows(database: SupportSQLiteDatabase, tableName: String): Long =
        database.query("SELECT COUNT(*) FROM $tableName").use { cursor ->
            assertTrue(cursor.moveToFirst())
            cursor.getLong(0)
        }

    /** Exact v1 schema reconstructed from app/schemas/.../1.json. */
    private class Version1Callback : SupportSQLiteOpenHelper.Callback(1) {
        override fun onCreate(database: SupportSQLiteDatabase) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `apps` (`packageName` TEXT NOT NULL, `label` TEXT NOT NULL, `category` TEXT, `isEssential` INTEGER NOT NULL, PRIMARY KEY(`packageName`))")
            database.execSQL("CREATE TABLE IF NOT EXISTS `app_categories` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `isSystemDefined` INTEGER NOT NULL)")
            database.execSQL("CREATE TABLE IF NOT EXISTS `daily_usage` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `packageName` TEXT NOT NULL, `epochDay` INTEGER NOT NULL, `totalUsageSeconds` INTEGER NOT NULL, `launchCount` INTEGER NOT NULL, `sessionCount` INTEGER NOT NULL)")
            database.execSQL("CREATE TABLE IF NOT EXISTS `sessions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `packageName` TEXT NOT NULL, `startTs` INTEGER NOT NULL, `endTs` INTEGER)")
            database.execSQL("CREATE TABLE IF NOT EXISTS `focus_sessions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `startTs` INTEGER NOT NULL, `endTs` INTEGER, `plannedMinutes` INTEGER NOT NULL, `completedMinutes` INTEGER NOT NULL, `blockedCategoryIds` TEXT NOT NULL)")
            database.execSQL("CREATE TABLE IF NOT EXISTS `rules` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `type` TEXT NOT NULL, `targetPackageOrCategory` TEXT NOT NULL, `threshold` INTEGER, `windowDefinition` TEXT)")
            database.execSQL("CREATE TABLE IF NOT EXISTS `schedules` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `daysOfWeek` TEXT NOT NULL, `startTime` TEXT NOT NULL, `endTime` TEXT NOT NULL, `associatedRuleId` INTEGER NOT NULL, FOREIGN KEY(`associatedRuleId`) REFERENCES `rules`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
            database.execSQL("CREATE TABLE IF NOT EXISTS `app_list_entries` (`packageName` TEXT NOT NULL, `listType` TEXT NOT NULL, PRIMARY KEY(`packageName`, `listType`))")
            database.execSQL("CREATE TABLE IF NOT EXISTS `goals` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `type` TEXT NOT NULL, `targetValue` INTEGER NOT NULL, `period` TEXT NOT NULL)")
            database.execSQL("CREATE TABLE IF NOT EXISTS `habit_records` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `epochDay` INTEGER NOT NULL, `goalId` INTEGER NOT NULL, `met` INTEGER NOT NULL, FOREIGN KEY(`goalId`) REFERENCES `goals`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
            database.execSQL("CREATE TABLE IF NOT EXISTS `notification_preferences` (`id` INTEGER NOT NULL, `quietWindows` TEXT NOT NULL, `digestEnabled` INTEGER NOT NULL, PRIMARY KEY(`id`))")
            database.execSQL("CREATE TABLE IF NOT EXISTS `theme_preferences` (`id` INTEGER NOT NULL, `themeId` TEXT NOT NULL, PRIMARY KEY(`id`))")
            database.execSQL("CREATE TABLE IF NOT EXISTS `user_preferences` (`key` TEXT NOT NULL, `value` TEXT NOT NULL, PRIMARY KEY(`key`))")
            database.execSQL("CREATE TABLE IF NOT EXISTS `local_recommendations` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `epochDay` INTEGER NOT NULL, `ruleSuggestion` TEXT NOT NULL, `basis` TEXT NOT NULL)")
            database.execSQL("CREATE TABLE IF NOT EXISTS `emergency_overrides` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `timestamp` INTEGER NOT NULL, `ruleId` INTEGER NOT NULL, `reason` TEXT)")
            database.execSQL("CREATE TABLE IF NOT EXISTS `privacy_settings` (`permissionName` TEXT NOT NULL, `granted` INTEGER NOT NULL, `lastChecked` INTEGER NOT NULL, PRIMARY KEY(`permissionName`))")
            database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_daily_usage_packageName_epochDay` ON `daily_usage` (`packageName`, `epochDay`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_schedules_associatedRuleId` ON `schedules` (`associatedRuleId`)")
            database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_habit_records_goalId_epochDay` ON `habit_records` (`goalId`, `epochDay`)")
            database.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
            database.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES (42, 'ea884de73bad199de34f5f370fc249cd')")
        }

        override fun onUpgrade(database: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
    }

    /** Exact v2 schema reconstructed from app/schemas/.../2.json (v1 plus
     * `schedules.name` and `focus_sessions.blockedPackages`, both `NOT NULL DEFAULT ''`). */
    private class Version2Callback : SupportSQLiteOpenHelper.Callback(2) {
        override fun onCreate(database: SupportSQLiteDatabase) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `apps` (`packageName` TEXT NOT NULL, `label` TEXT NOT NULL, `category` TEXT, `isEssential` INTEGER NOT NULL, PRIMARY KEY(`packageName`))")
            database.execSQL("CREATE TABLE IF NOT EXISTS `app_categories` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `isSystemDefined` INTEGER NOT NULL)")
            database.execSQL("CREATE TABLE IF NOT EXISTS `daily_usage` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `packageName` TEXT NOT NULL, `epochDay` INTEGER NOT NULL, `totalUsageSeconds` INTEGER NOT NULL, `launchCount` INTEGER NOT NULL, `sessionCount` INTEGER NOT NULL)")
            database.execSQL("CREATE TABLE IF NOT EXISTS `sessions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `packageName` TEXT NOT NULL, `startTs` INTEGER NOT NULL, `endTs` INTEGER)")
            database.execSQL("CREATE TABLE IF NOT EXISTS `focus_sessions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `startTs` INTEGER NOT NULL, `endTs` INTEGER, `plannedMinutes` INTEGER NOT NULL, `completedMinutes` INTEGER NOT NULL, `blockedCategoryIds` TEXT NOT NULL, `blockedPackages` TEXT NOT NULL)")
            database.execSQL("CREATE TABLE IF NOT EXISTS `rules` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `type` TEXT NOT NULL, `targetPackageOrCategory` TEXT NOT NULL, `threshold` INTEGER, `windowDefinition` TEXT)")
            database.execSQL("CREATE TABLE IF NOT EXISTS `schedules` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `daysOfWeek` TEXT NOT NULL, `startTime` TEXT NOT NULL, `endTime` TEXT NOT NULL, `associatedRuleId` INTEGER NOT NULL, FOREIGN KEY(`associatedRuleId`) REFERENCES `rules`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
            database.execSQL("CREATE TABLE IF NOT EXISTS `app_list_entries` (`packageName` TEXT NOT NULL, `listType` TEXT NOT NULL, PRIMARY KEY(`packageName`, `listType`))")
            database.execSQL("CREATE TABLE IF NOT EXISTS `goals` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `type` TEXT NOT NULL, `targetValue` INTEGER NOT NULL, `period` TEXT NOT NULL)")
            database.execSQL("CREATE TABLE IF NOT EXISTS `habit_records` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `epochDay` INTEGER NOT NULL, `goalId` INTEGER NOT NULL, `met` INTEGER NOT NULL, FOREIGN KEY(`goalId`) REFERENCES `goals`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
            database.execSQL("CREATE TABLE IF NOT EXISTS `notification_preferences` (`id` INTEGER NOT NULL, `quietWindows` TEXT NOT NULL, `digestEnabled` INTEGER NOT NULL, PRIMARY KEY(`id`))")
            database.execSQL("CREATE TABLE IF NOT EXISTS `theme_preferences` (`id` INTEGER NOT NULL, `themeId` TEXT NOT NULL, PRIMARY KEY(`id`))")
            database.execSQL("CREATE TABLE IF NOT EXISTS `user_preferences` (`key` TEXT NOT NULL, `value` TEXT NOT NULL, PRIMARY KEY(`key`))")
            database.execSQL("CREATE TABLE IF NOT EXISTS `local_recommendations` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `epochDay` INTEGER NOT NULL, `ruleSuggestion` TEXT NOT NULL, `basis` TEXT NOT NULL)")
            database.execSQL("CREATE TABLE IF NOT EXISTS `emergency_overrides` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `timestamp` INTEGER NOT NULL, `ruleId` INTEGER NOT NULL, `reason` TEXT)")
            database.execSQL("CREATE TABLE IF NOT EXISTS `privacy_settings` (`permissionName` TEXT NOT NULL, `granted` INTEGER NOT NULL, `lastChecked` INTEGER NOT NULL, PRIMARY KEY(`permissionName`))")
            database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_daily_usage_packageName_epochDay` ON `daily_usage` (`packageName`, `epochDay`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_schedules_associatedRuleId` ON `schedules` (`associatedRuleId`)")
            database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_habit_records_goalId_epochDay` ON `habit_records` (`goalId`, `epochDay`)")
            database.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
            database.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES (42, '5de2ce6aaf1849ed07085bd237a7a2c9')")
        }

        override fun onUpgrade(database: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
    }

    private companion object {
        const val DATABASE_NAME = "orlune-migration-test.db"
    }
}
