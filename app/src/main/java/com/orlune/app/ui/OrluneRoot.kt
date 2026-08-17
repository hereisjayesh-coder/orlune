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
import com.orlune.app.core.domain.focus.FocusSessionEngine
import com.orlune.app.core.domain.focus.FocusSessionState
import com.orlune.app.data.local.entity.RuleEntity
import com.orlune.app.data.local.entity.ScheduleEntity
import com.orlune.app.data.local.entity.ThemePreferenceEntity
import com.orlune.app.data.privacy.LocalDataExporter
import com.orlune.app.feature.focus.FocusSection
import com.orlune.app.feature.home.HomeScreen
import com.orlune.app.feature.insights.InsightsScreen
import com.orlune.app.feature.limits.LimitsSection
import com.orlune.app.feature.settings.SettingsSection
import com.orlune.app.platform.blocking.BlockingMonitorService
import com.orlune.app.platform.blocking.NotificationPermission
import com.orlune.app.platform.blocking.OverlayPermission
import com.orlune.app.platform.feedback.FeedbackIntent
import com.orlune.app.platform.notifications.NotificationPolicyAccessPermission
import com.orlune.app.platform.usage.UsageAccessPermission
import com.orlune.app.ui.navigation.OrluneTab
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

@Composable
fun OrluneRoot(app: OrluneApplication) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedTab by rememberSaveable { mutableStateOf(OrluneTab.HOME.name) }
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
    val rules by app.database.ruleDao().observeAll().collectAsState(initial = emptyList())
    val apps by app.database.appDao().observeAll().collectAsState(initial = emptyList())
    val focusSessions by app.focusSessionRepository.observeAll().collectAsState(initial = emptyList())
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
                    sessions = focusSessions,
                    usageAccessGranted = usageAccessGranted,
                    overlayGranted = overlayGranted,
                    notificationPolicyAccessGranted = notificationPolicyAccessGranted,
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
                            app.database.ruleDao().upsert(
                                RuleEntity(type = "limit", targetPackageOrCategory = packageName, threshold = seconds, windowDefinition = null)
                            )
                        }
                    },
                    onAddSchedule = { name, packageName, days, start, end ->
                        scope.launch {
                            val id = app.database.ruleDao().upsert(
                                RuleEntity(type = "schedule", targetPackageOrCategory = packageName, threshold = null, windowDefinition = null)
                            )
                            app.database.scheduleDao().upsert(ScheduleEntity(name = name, daysOfWeek = days, startTime = start, endTime = end, associatedRuleId = id))
                        }
                    },
                    onDelete = { rule -> scope.launch { app.database.ruleDao().delete(rule) } }
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
                    insightsPeriodEndExclusiveMillis = insightsPeriodEndMillisExclusive
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
                        app.database.clearAllTables()
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
                        app.database.clearAllTables()
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
