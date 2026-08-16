package com.orlune.app

import android.app.Application
import androidx.room.Room
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.orlune.app.core.domain.focus.FocusSessionEngine
import com.orlune.app.core.domain.focus.FocusSessionState
import com.orlune.app.data.local.OrluneDatabase
import com.orlune.app.data.local.OrluneMigrations
import com.orlune.app.data.repository.BlockingRepository
import com.orlune.app.data.repository.FocusSessionRepository
import com.orlune.app.data.repository.UsageRepository
import com.orlune.app.platform.blocking.BlockingMonitorService
import com.orlune.app.platform.blocking.OverlayPermission
import com.orlune.app.platform.usage.AppLabelResolver
import com.orlune.app.platform.usage.UsageAccessPermission
import com.orlune.app.platform.usage.UsageEventReader
import com.orlune.app.platform.usage.worker.OrluneWorkerFactory
import com.orlune.app.platform.usage.worker.UsageAggregationWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class OrluneApplication : Application(), Configuration.Provider {

    val database: OrluneDatabase by lazy {
        // Version 1 -> 2 uses an explicit preserving migration; unsupported upgrades
        // must fail rather than silently deleting local user data.
        // has ever been written (see OrluneDatabase's version-2 KDoc) — an honest
        // pre-release posture, not a shortcut around a real migration.
        Room.databaseBuilder(this, OrluneDatabase::class.java, OrluneDatabase.DATABASE_NAME)
            .addMigrations(OrluneMigrations.MIGRATION_1_2)
            .build()
    }

    val usageRepository: UsageRepository by lazy {
        UsageRepository(
            usageEventSource = UsageEventReader(this),
            appLabelSource = AppLabelResolver(this),
            appDao = database.appDao(),
            sessionDao = database.sessionDao(),
            dailyUsageDao = database.dailyUsageDao(),
            userPreferenceDao = database.userPreferenceDao()
        )
    }

    val blockingRepository: BlockingRepository by lazy {
        BlockingRepository(
            ruleDao = database.ruleDao(),
            scheduleDao = database.scheduleDao(),
            appListEntryDao = database.appListEntryDao(),
            dailyUsageDao = database.dailyUsageDao(),
            sessionDao = database.sessionDao(),
            focusSessionDao = database.focusSessionDao(),
            ownPackageName = packageName
        )
    }

    val focusSessionRepository: FocusSessionRepository by lazy {
        FocusSessionRepository(focusSessionDao = database.focusSessionDao())
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(OrluneWorkerFactory(usageRepository))
            .build()

    override fun onCreate() {
        super.onCreate()
        schedulePeriodicUsageAggregation()
        resumeMonitoringIfNeeded()
    }

    /**
     * One-shot check at process start, not a persistent watcher: if a rule or focus
     * session already exists and both required permissions are already granted,
     * restart monitoring automatically so a normal app restart (including a fresh
     * process after an OEM kill, once something does restart it) doesn't require a
     * manual re-toggle. Never fights the OS to stay alive — it just makes recovery
     * automatic when the process does come back.
     */
    private fun resumeMonitoringIfNeeded() {
        if (!UsageAccessPermission.isGranted(this) || !OverlayPermission.isGranted(this)) return
        CoroutineScope(Dispatchers.Default).launch {
            val hasRules = database.ruleDao().observeAll().first().isNotEmpty()
            val now = System.currentTimeMillis()
            val hasFocusSession = database.focusSessionDao().observeAll().first().any {
                val state = FocusSessionEngine.stateOf(it, now)
                state == FocusSessionState.ACTIVE || state == FocusSessionState.SCHEDULED
            }
            if (hasRules || hasFocusSession) {
                BlockingMonitorService.start(this@OrluneApplication)
            }
        }
    }

    /**
     * 15 minutes is WorkManager's minimum periodic interval — there's no
     * blocking-latency requirement in this phase to justify anything tighter (see
     * UsageAggregationWorker). KEEP means an already-enqueued schedule survives app
     * upgrades/reboots without being reset.
     */
    private fun schedulePeriodicUsageAggregation() {
        val request = PeriodicWorkRequestBuilder<UsageAggregationWorker>(15, TimeUnit.MINUTES).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            UsageAggregationWorker.UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}
