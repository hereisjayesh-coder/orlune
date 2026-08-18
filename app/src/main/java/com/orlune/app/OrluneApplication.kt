package com.orlune.app

import android.app.Application
import androidx.room.Room
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.orlune.app.core.domain.focus.FocusNotificationPolicy
import com.orlune.app.core.domain.focus.FocusSessionEngine
import com.orlune.app.core.domain.focus.FocusSessionState
import com.orlune.app.core.domain.focus.effectiveFocusNotificationState
import com.orlune.app.data.local.OrluneDatabase
import com.orlune.app.data.local.OrluneMigrations
import com.orlune.app.data.repository.BlockingRepository
import com.orlune.app.data.repository.FocusSessionRepository
import com.orlune.app.data.repository.OnboardingRepository
import com.orlune.app.data.repository.RuleRepository
import com.orlune.app.data.repository.UsageRepository
import com.orlune.app.platform.blocking.BlockingMonitorService
import com.orlune.app.platform.blocking.OverlayPermission
import com.orlune.app.platform.notifications.FocusZenRuleController
import com.orlune.app.platform.usage.AppLabelResolver
import com.orlune.app.platform.usage.InstalledAppLister
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
        // Version 1 -> 2 uses an explicit preserving migration (see OrluneMigrations.kt);
        // unsupported upgrades must fail rather than silently deleting local user data.
        Room.databaseBuilder(this, OrluneDatabase::class.java, OrluneDatabase.DATABASE_NAME)
            .addMigrations(
                OrluneMigrations.MIGRATION_1_2,
                OrluneMigrations.MIGRATION_2_3,
                OrluneMigrations.MIGRATION_3_4,
                OrluneMigrations.MIGRATION_4_5
            )
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

    val installedAppLister: InstalledAppLister by lazy { InstalledAppLister(this) }

    val blockingRepository: BlockingRepository by lazy {
        BlockingRepository(
            ruleDao = database.ruleDao(),
            scheduleDao = database.scheduleDao(),
            appListEntryDao = database.appListEntryDao(),
            dailyUsageDao = database.dailyUsageDao(),
            sessionDao = database.sessionDao(),
            focusSessionDao = database.focusSessionDao(),
            ruleSnoozeDao = database.ruleSnoozeDao(),
            ownPackageName = packageName
        )
    }

    val focusSessionRepository: FocusSessionRepository by lazy {
        FocusSessionRepository(focusSessionDao = database.focusSessionDao())
    }

    val onboardingRepository: OnboardingRepository by lazy {
        OnboardingRepository(onboardingStateDao = database.onboardingStateDao())
    }

    val ruleRepository: RuleRepository by lazy {
        RuleRepository(
            ruleDao = database.ruleDao(),
            scheduleDao = database.scheduleDao(),
            ruleSnoozeDao = database.ruleSnoozeDao()
        )
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(OrluneWorkerFactory(usageRepository))
            .build()

    override fun onCreate() {
        super.onCreate()
        schedulePeriodicUsageAggregation()
        resumeMonitoringIfNeeded()
        reconcileFocusNotificationPolicyOnColdStart()
        backfillOnboardingCompletionForExistingInstalls()
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
     * A stray Orlune Zen rule left `STATE_TRUE` cannot rely on
     * [BlockingMonitorService]'s tick loop alone to ever turn it back off: that loop
     * only keeps running while [resumeMonitoringIfNeeded] finds live work (a rule or
     * an ACTIVE/SCHEDULED session) — a session that already finished (or that a killed
     * process's [reconcileActiveSessions] never got to finalize) leaves nothing to
     * restart the service for, so the rule would stay silently active forever with no
     * later tick ever reaching the code that turns it off. This runs unconditionally,
     * independent of Usage Access/Overlay (notification policy has nothing to do with
     * either), so a device reboot or a process kill mid-session always gets a chance
     * to restore normal notification behavior the next time Orlune's process starts —
     * even if that's just from the user reopening the app, not a background restart.
     */
    private fun reconcileFocusNotificationPolicyOnColdStart() {
        CoroutineScope(Dispatchers.Default).launch {
            focusSessionRepository.reconcileActiveSessions()
            val sessions = database.focusSessionDao().observeAll().first()
            val effective = effectiveFocusNotificationState(sessions, System.currentTimeMillis())
            FocusZenRuleController.reconcile(this@OrluneApplication, effective?.policy)
        }
    }

    /**
     * Onboarding (this session) is a brand-new table (`OnboardingStateEntity`, Room
     * v3→v4) — an install that already existed before this feature shipped has no
     * row there at all after upgrading, which [OnboardingRepository.observeCompleted]
     * would otherwise read as "first launch, show onboarding." That's wrong:
     * onboarding is a first-*launch* concept, not a first-*app-version* one, and an
     * established user updating to this version must never suddenly be dropped into
     * a first-run wizard. This one-shot check runs before anything reads the
     * onboarding flag: if no onboarding row exists yet AND there's real evidence of
     * prior use (any usage session, rule, or focus session ever recorded), it
     * silently backfills a completed row with empty/default choices — never shown to
     * the user, indistinguishable afterward from having skipped every optional
     * onboarding choice. A genuinely fresh install has zero rows in all three tables,
     * so onboarding still shows normally.
     */
    private fun backfillOnboardingCompletionForExistingInstalls() {
        CoroutineScope(Dispatchers.Default).launch {
            val alreadyRecorded = database.onboardingStateDao().observe().first() != null
            if (alreadyRecorded) return@launch
            val hasExistingData = database.sessionDao().observeCount().first() > 0 ||
                database.ruleDao().observeAll().first().isNotEmpty() ||
                database.focusSessionDao().observeAll().first().isNotEmpty()
            if (hasExistingData) {
                onboardingRepository.complete(
                    goals = emptySet(),
                    customGoalText = "",
                    focusNotificationPreference = FocusNotificationPolicy.ALLOW_ALL
                )
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
