package com.orlune.app.ui

import android.content.ActivityNotFoundException
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.orlune.app.OrluneApplication
import com.orlune.app.core.domain.focus.FocusNotificationPolicy
import com.orlune.app.core.domain.focus.FocusSessionEngine
import com.orlune.app.core.domain.focus.FocusSessionState
import com.orlune.app.data.local.entity.ThemePreferenceEntity
import com.orlune.app.data.privacy.LocalDataExporter
import com.orlune.app.feature.focus.FocusSection
import com.orlune.app.feature.home.HomeScreen
import com.orlune.app.feature.insights.InsightsScreen
import com.orlune.app.feature.limits.LimitsSection
import com.orlune.app.feature.onboarding.OnboardingSection
import com.orlune.app.feature.settings.SettingsSection
import com.orlune.app.platform.blocking.BlockingMonitorService
import com.orlune.app.platform.blocking.NotificationPermission
import com.orlune.app.platform.blocking.OverlayPermission
import com.orlune.app.platform.feedback.FeedbackIntent
import com.orlune.app.platform.notifications.NotificationPolicyAccessPermission
import com.orlune.app.platform.usage.UsageAccessPermission
import com.orlune.app.ui.components.orluneSafeAreaPadding
import com.orlune.app.ui.navigation.OrluneTab
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId

@Composable
fun OrluneRoot(app: OrluneApplication, openFocusForPackage: String? = null) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // A one-shot cold-start hint from the block screen's "Start Focus" button (see
    // MainActivity.EXTRA_OPEN_FOCUS_FOR_PACKAGE) — read once into the tab's initial
    // value, same as [selectedTab] itself only seeds from HOME once. Not re-applied on
    // every recomposition/tab switch, so navigating away from Focus afterward doesn't
    // keep snapping back to it.
    var selectedTab by rememberSaveable { mutableStateOf((if (openFocusForPackage != null) OrluneTab.FOCUS else OrluneTab.HOME).name) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    var exportError by remember { mutableStateOf(false) }
    var noEmailAppAvailable by remember { mutableStateOf(false) }
    val tab = OrluneTab.valueOf(selectedTab)

    val today = LocalDate.now().toEpochDay()
    val weekStart = today - 6
    val previousWeekStart = today - 13
    val previousWeekEnd = today - 7
    val todayUsage by app.usageRepository.observeTodayUsage().collectAsState(initial = emptyList())
    val lastWeekTotal by app.database.dailyUsageDao()
        .observeTotalSecondsBetween(weekStart, today)
        .collectAsState(initial = 0L)
    val previousWeekTotal by app.database.dailyUsageDao()
        .observeTotalSecondsBetween(previousWeekStart, previousWeekEnd)
        .collectAsState(initial = 0L)
    val comparisonApps by app.database.dailyUsageDao()
        .observeAppTotalsBetween(previousWeekStart, today)
        .collectAsState(initial = emptyList())
    // 4 consecutive 7-day buckets ending today, for Insights' "4 weeks" range — one
    // query, InsightsMetrics.weeklyBreakdown does the bucketing itself (see its KDoc).
    val fourWeeksStart = today - 27
    val fourWeekDayRows by app.database.dailyUsageDao()
        .observeAppDailyUsageBetween(fourWeeksStart, today)
        .collectAsState(initial = emptyList())
    val rules by app.database.ruleDao().observeAll().collectAsState(initial = emptyList())
    val apps by app.database.appDao().observeAll().collectAsState(initial = emptyList())
    val focusSessions by app.focusSessionRepository.observeAll().collectAsState(initial = emptyList())
    val onboardingFocusNotificationPreference by app.onboardingRepository.observeFocusNotificationPreference()
        .collectAsState(initial = FocusNotificationPolicy.ALLOW_ALL)
    val themePreference by app.database.themePreferenceDao().observe().collectAsState(initial = null)
    val sessionCount by app.database.sessionDao().observeCount().collectAsState(initial = 0)
    val dailyUsageCount by app.database.dailyUsageDao().observeCount().collectAsState(initial = 0)
    val todayUsageSecondsByPackage = remember(todayUsage) { todayUsage.associate { it.packageName to it.totalUsageSeconds } }

    // Same "last 14 days" window Insights already uses for comparisonApps —
    // the longest-session/focus-session facts stay scoped to the same period the
    // apps list is already labeled with, rather than introducing a second window.
    val zoneId = remember { ZoneId.systemDefault() }
    val insightsPeriodStartMillis = remember(previousWeekStart, zoneId) {
        LocalDate.ofEpochDay(previousWeekStart).atStartOfDay(zoneId).toInstant().toEpochMilli()
    }
    val insightsPeriodEndMillisExclusive = remember(today, zoneId) {
        LocalDate.ofEpochDay(today + 1).atStartOfDay(zoneId).toInstant().toEpochMilli()
    }
    val longestSession by app.database.sessionDao()
        .observeLongestSessionBetween(insightsPeriodStartMillis, insightsPeriodEndMillisExclusive)
        .collectAsState(initial = null)

    var usageAccessGranted by remember { mutableStateOf(UsageAccessPermission.isGranted(context)) }
    var overlayGranted by remember { mutableStateOf(OverlayPermission.isGranted(context)) }
    var notificationGranted by remember { mutableStateOf(NotificationPermission.isGranted(context)) }
    var notificationPolicyAccessGranted by remember { mutableStateOf(NotificationPolicyAccessPermission.isGranted(context)) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                usageAccessGranted = UsageAccessPermission.isGranted(context)
                overlayGranted = OverlayPermission.isGranted(context)
                notificationGranted = NotificationPermission.isGranted(context)
                notificationPolicyAccessGranted = NotificationPolicyAccessPermission.isGranted(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Defaults to "not completed" for the one frame before the real Room value
    // arrives — on a true fresh install this is already correct; on an install
    // upgrading into this feature for the first time, `backfillOnboardingCompletionForExistingInstalls`
    // corrects it within the same cold start (a brief, one-time, self-correcting
    // flash, not a functional issue — see docs/PROJECT_STATE.md).
    val onboardingCompleted by app.onboardingRepository.observeCompleted().collectAsState(initial = false)

    if (!onboardingCompleted) {
        // Same Surface(background) wrapping the tab content below gets for free —
        // without it, Text without an explicit color falls back to Compose's default
        // LocalContentColor (black), invisible against this app's black background.
        // Confirmed on-device: the Welcome headline was fully rendered but invisible
        // before this fix, since Surface is what actually establishes the correct
        // LocalContentColor for un-colored Text, not MaterialTheme alone.
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            // The Surface itself stays full-bleed (background color extends behind
            // the status/nav bars, matching this app's edge-to-edge black-first
            // look) — only the actual content is pushed clear of the system bars.
            OnboardingSection(
                modifier = Modifier.fillMaxSize().orluneSafeAreaPadding(),
                installedAppSource = app.installedAppLister,
                ownPackageName = context.packageName,
                todayUsageSecondsByPackage = todayUsageSecondsByPackage,
                usageAccessGranted = usageAccessGranted,
                overlayGranted = overlayGranted,
                notificationPolicyAccessGranted = notificationPolicyAccessGranted,
                onOpenUsageAccessSettings = { context.startActivity(UsageAccessPermission.settingsIntent()) },
                onOpenOverlaySettings = { context.startActivity(OverlayPermission.settingsIntent(context)) },
                onOpenNotificationPolicySettings = { context.startActivity(NotificationPolicyAccessPermission.settingsIntent()) },
                onComplete = { goals, customGoalText, focusNotificationPreference, dailyLimitPlan ->
                    // Single coroutine, sequential: the rule(s) land in Room before
                    // onboarding is marked complete, not as independent fire-and-forget
                    // launches racing each other — see OnboardingDailyLimit's KDoc for
                    // the bug this replaced (a duration could silently vanish).
                    scope.launch {
                        if (dailyLimitPlan != null) {
                            dailyLimitPlan.packages.forEach { packageName ->
                                app.ruleRepository.addDailyLimit(packageName, dailyLimitPlan.thresholdSeconds)
                            }
                        }
                        app.onboardingRepository.complete(goals, customGoalText, focusNotificationPreference)
                        if (dailyLimitPlan != null) {
                            // A rule was just created while the app is already running —
                            // BlockingMonitorService only auto-starts at process cold
                            // start (OrluneApplication.resumeMonitoringIfNeeded), so a
                            // freshly-added rule needs this same explicit start the
                            // Focus tab's onStart already does below, or it silently
                            // wouldn't be enforced until the next full app restart.
                            BlockingMonitorService.start(context)
                        }
                    }
                }
            )
        }
        return
    }

    Scaffold(
        bottomBar = {
            NavigationBar(modifier = Modifier.navigationBarsPadding()) {
                OrluneTab.entries.forEach { destination ->
                    NavigationBarItem(
                        selected = tab == destination,
                        onClick = { selectedTab = destination.name },
                        icon = { Icon(destination.icon, contentDescription = destination.label) },
                        label = { Text(destination.label) }
                    )
                }
            }
        }
    ) { padding ->
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            when (tab) {
                OrluneTab.HOME -> HomeScreen(
                    modifier = Modifier.padding(padding),
                    todayUsage = todayUsage,
                    totalToday = todayUsage.sumOf { it.totalUsageSeconds },
                    activeRules = rules.count { it.type == "limit" || it.type == "schedule" },
                    activeFocus = focusSessions.firstOrNull { FocusSessionEngine.stateOf(it, System.currentTimeMillis()) == FocusSessionState.ACTIVE },
                    usageAccessGranted = usageAccessGranted,
                    installedAppSource = app.installedAppLister,
                    ownPackageName = context.packageName,
                    onOpenUsageAccess = { context.startActivity(UsageAccessPermission.settingsIntent()) },
                    onRefresh = { scope.launch { app.usageRepository.processNewEvents() } },
                    onFocus = { selectedTab = OrluneTab.FOCUS.name }
                )
                OrluneTab.FOCUS -> FocusSection(
                    modifier = Modifier.padding(padding),
                    installedAppSource = app.installedAppLister,
                    ownPackageName = context.packageName,
                    todayUsageSecondsByPackage = todayUsageSecondsByPackage,
                    initialSelectedPackage = openFocusForPackage,
                    sessions = focusSessions,
                    usageAccessGranted = usageAccessGranted,
                    overlayGranted = overlayGranted,
                    notificationPolicyAccessGranted = notificationPolicyAccessGranted,
                    initialNotificationPolicy = onboardingFocusNotificationPreference,
                    onOpenOverlay = { context.startActivity(OverlayPermission.settingsIntent(context)) },
                    onOpenNotificationPolicySettings = { context.startActivity(NotificationPolicyAccessPermission.settingsIntent()) },
                    onStart = { minutes, packages, notificationPolicy, allowedNotificationPackages ->
                        scope.launch {
                            app.focusSessionRepository.startSession(
                                plannedMinutes = minutes,
                                blockedPackages = packages,
                                notificationPolicy = notificationPolicy,
                                allowedNotificationPackages = allowedNotificationPackages
                            )
                            BlockingMonitorService.start(context)
                        }
                    },
                    onStop = { scope.launch { app.focusSessionRepository.cancelActiveSessions() } }
                )
                OrluneTab.LIMITS -> LimitsSection(
                    modifier = Modifier.padding(padding),
                    apps = apps,
                    rules = rules,
                    installedAppSource = app.installedAppLister,
                    ownPackageName = context.packageName,
                    todayUsageSecondsByPackage = todayUsageSecondsByPackage,
                    onAddLimit = { packageName, seconds ->
                        scope.launch {
                            app.ruleRepository.addDailyLimit(packageName, seconds)
                            // Same reasoning as onboarding's onComplete above: a rule
                            // just appeared while Orlune is already running, and the
                            // monitor service only auto-starts at process cold start.
                            BlockingMonitorService.start(context)
                        }
                    },
                    onAddSchedule = { name, packageName, days, start, end ->
                        scope.launch {
                            app.ruleRepository.addSchedule(name, packageName, days, start, end)
                            BlockingMonitorService.start(context)
                        }
                    },
                    onDelete = { rule -> scope.launch { app.ruleRepository.delete(rule) } }
                )
                OrluneTab.INSIGHTS -> InsightsScreen(
                    modifier = Modifier.padding(padding),
                    lastWeekTotal = lastWeekTotal,
                    previousWeekTotal = previousWeekTotal,
                    apps = comparisonApps,
                    installedAppSource = app.installedAppLister,
                    ownPackageName = context.packageName,
                    rules = rules,
                    todayUsage = todayUsage,
                    focusSessions = focusSessions,
                    longestSession = longestSession,
                    insightsPeriodStartMillis = insightsPeriodStartMillis,
                    insightsPeriodEndExclusiveMillis = insightsPeriodEndMillisExclusive,
                    fourWeekDayRows = fourWeekDayRows,
                    today = today,
                    zoneId = zoneId
                )
                OrluneTab.SETTINGS -> SettingsSection(
                    modifier = Modifier.padding(padding),
                    themeMode = themePreference?.themeId ?: "system",
                    usageAccessGranted = usageAccessGranted,
                    overlayGranted = overlayGranted,
                    notificationGranted = notificationGranted,
                    onThemeChange = { mode ->
                        scope.launch { app.database.themePreferenceDao().upsert(ThemePreferenceEntity(themeId = mode)) }
                    },
                    onOpenUsageAccess = { context.startActivity(UsageAccessPermission.settingsIntent()) },
                    onOpenOverlay = { context.startActivity(OverlayPermission.settingsIntent(context)) },
                    onExport = {
                        scope.launch {
                            runCatching { LocalDataExporter.share(context, app.database) }
                                .onFailure { exportError = true }
                        }
                    },
                    onDeleteRequest = { showDeleteDialog = true },
                    onResetRequest = { showResetDialog = true },
                    onOpenFeedback = {
                        try {
                            context.startActivity(FeedbackIntent.compose())
                        } catch (e: ActivityNotFoundException) {
                            noEmailAppAvailable = true
                        }
                    },
                    sessionCount = sessionCount,
                    dailyUsageCount = dailyUsageCount,
                    ruleCount = rules.size,
                    focusSessionCount = focusSessions.size,
                    knownAppCount = apps.size
                )
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete local data?") },
            text = { Text("This deletes usage history, rules, schedules, focus sessions, and preferences from this device. It cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    scope.launch {
                        BlockingMonitorService.stop(context)
                        withContext(Dispatchers.IO) { app.database.clearAllTables() }
                    }
                }) { Text("Delete all") }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") } }
        )
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset Orlune?") },
            text = { Text("This returns Orlune to its initial, freshly-installed state — usage history, rules, schedules, focus sessions, and preferences are all cleared. It cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    showResetDialog = false
                    scope.launch {
                        BlockingMonitorService.stop(context)
                        withContext(Dispatchers.IO) { app.database.clearAllTables() }
                    }
                }) { Text("Reset") }
            },
            dismissButton = { TextButton(onClick = { showResetDialog = false }) { Text("Cancel") } }
        )
    }

    if (exportError) {
        AlertDialog(
            onDismissRequest = { exportError = false },
            title = { Text("Export unavailable") },
            text = { Text("Orlune could not create the local export file. Your stored data was not changed.") },
            confirmButton = { TextButton(onClick = { exportError = false }) { Text("OK") } }
        )
    }

    if (noEmailAppAvailable) {
        AlertDialog(
            onDismissRequest = { noEmailAppAvailable = false },
            title = { Text("No email app found") },
            text = { Text("No email app is available on this device.") },
            confirmButton = { TextButton(onClick = { noEmailAppAvailable = false }) { Text("OK") } }
        )
    }
}
