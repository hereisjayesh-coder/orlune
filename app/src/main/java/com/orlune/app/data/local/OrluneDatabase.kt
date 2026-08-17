package com.orlune.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.orlune.app.data.local.dao.AppCategoryDao
import com.orlune.app.data.local.dao.AppDao
import com.orlune.app.data.local.dao.AppListEntryDao
import com.orlune.app.data.local.dao.DailyUsageDao
import com.orlune.app.data.local.dao.EmergencyOverrideDao
import com.orlune.app.data.local.dao.FocusSessionDao
import com.orlune.app.data.local.dao.GoalDao
import com.orlune.app.data.local.dao.HabitRecordDao
import com.orlune.app.data.local.dao.LocalRecommendationDao
import com.orlune.app.data.local.dao.NotificationPreferenceDao
import com.orlune.app.data.local.dao.PrivacySettingDao
import com.orlune.app.data.local.dao.RuleDao
import com.orlune.app.data.local.dao.ScheduleDao
import com.orlune.app.data.local.dao.SessionDao
import com.orlune.app.data.local.dao.ThemePreferenceDao
import com.orlune.app.data.local.dao.UserPreferenceDao
import com.orlune.app.data.local.entity.AppCategoryEntity
import com.orlune.app.data.local.entity.AppEntity
import com.orlune.app.data.local.entity.AppListEntryEntity
import com.orlune.app.data.local.entity.DailyUsageEntity
import com.orlune.app.data.local.entity.EmergencyOverrideEntity
import com.orlune.app.data.local.entity.FocusSessionEntity
import com.orlune.app.data.local.entity.GoalEntity
import com.orlune.app.data.local.entity.HabitRecordEntity
import com.orlune.app.data.local.entity.LocalRecommendationEntity
import com.orlune.app.data.local.entity.NotificationPreferenceEntity
import com.orlune.app.data.local.entity.PrivacySettingEntity
import com.orlune.app.data.local.entity.RuleEntity
import com.orlune.app.data.local.entity.ScheduleEntity
import com.orlune.app.data.local.entity.SessionEntity
import com.orlune.app.data.local.entity.ThemePreferenceEntity
import com.orlune.app.data.local.entity.UserPreferenceEntity

/**
 * Room database for Orlune. This is the entire local data model from
 * `docs/phase-0-research.md` Section 8 — schema only, per Phase 2's exit criteria.
 * No repository/domain layer consumes this yet, and nothing in `feature/` reads
 * from it — that starts in Phase 3+.
 *
 * Never backed up: see `AndroidManifest.xml` (`android:allowBackup="false"`) and
 * `xml/data_extraction_rules.xml`, both part of the local-only privacy architecture
 * (Section 10), not this class's concern to re-enforce.
 *
 * version 2 (Phase 6): `ScheduleEntity.name` and `FocusSessionEntity.blockedPackages`
 * added, both as `NOT NULL DEFAULT ''` so existing rows survive untouched.
 *
 * version 3 (Phase 8, Focus notification/quiet mode): `FocusSessionEntity.notificationPolicy`
 * (`NOT NULL DEFAULT 'ALLOW_ALL'`) and `FocusSessionEntity.allowedNotificationPackages`
 * (`NOT NULL DEFAULT ''`) added — see `core/domain/focus/FocusNotificationPolicy.kt` and
 * `docs/android-notification-policy.md`. `OrluneApplication`'s `Room.databaseBuilder`
 * wires up the explicit, data-preserving `OrluneMigrations.MIGRATION_1_2` and
 * `MIGRATION_2_3` — never `fallbackToDestructiveMigration()` — and
 * `OrluneDatabaseMigrationTest` verifies every prior-version table's rows survive each
 * upgrade. See `OrluneMigrations.kt`.
 */
@Database(
    entities = [
        AppEntity::class,
        AppCategoryEntity::class,
        DailyUsageEntity::class,
        SessionEntity::class,
        FocusSessionEntity::class,
        RuleEntity::class,
        ScheduleEntity::class,
        AppListEntryEntity::class,
        GoalEntity::class,
        HabitRecordEntity::class,
        NotificationPreferenceEntity::class,
        ThemePreferenceEntity::class,
        UserPreferenceEntity::class,
        LocalRecommendationEntity::class,
        EmergencyOverrideEntity::class,
        PrivacySettingEntity::class
    ],
    version = 3,
    exportSchema = true
)
abstract class OrluneDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao
    abstract fun appCategoryDao(): AppCategoryDao
    abstract fun dailyUsageDao(): DailyUsageDao
    abstract fun sessionDao(): SessionDao
    abstract fun focusSessionDao(): FocusSessionDao
    abstract fun ruleDao(): RuleDao
    abstract fun scheduleDao(): ScheduleDao
    abstract fun appListEntryDao(): AppListEntryDao
    abstract fun goalDao(): GoalDao
    abstract fun habitRecordDao(): HabitRecordDao
    abstract fun notificationPreferenceDao(): NotificationPreferenceDao
    abstract fun themePreferenceDao(): ThemePreferenceDao
    abstract fun userPreferenceDao(): UserPreferenceDao
    abstract fun localRecommendationDao(): LocalRecommendationDao
    abstract fun emergencyOverrideDao(): EmergencyOverrideDao
    abstract fun privacySettingDao(): PrivacySettingDao

    companion object {
        const val DATABASE_NAME = "orlune.db"
    }
}
