package com.orlune.app.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.orlune.app.OrluneApplication
import com.orlune.app.core.domain.focus.FocusSessionEngine
import com.orlune.app.core.domain.focus.FocusSessionState
import com.orlune.app.data.local.dao.AppDailyUsage
import com.orlune.app.data.local.dao.AppPeriodUsage
import com.orlune.app.data.local.entity.AppEntity
import com.orlune.app.data.local.entity.FocusSessionEntity
import com.orlune.app.data.local.entity.RuleEntity
import com.orlune.app.data.local.entity.ScheduleEntity
import com.orlune.app.data.local.entity.ThemePreferenceEntity
import com.orlune.app.data.privacy.LocalDataExporter
import com.orlune.app.platform.blocking.BlockingMonitorService
import com.orlune.app.platform.blocking.NotificationPermission
import com.orlune.app.platform.blocking.OverlayPermission
import com.orlune.app.platform.usage.UsageAccessPermission
import kotlinx.coroutines.launch
import java.time.LocalDate

private enum class OrluneTab(val label: String) {
    HOME("Home"),
    FOCUS("Focus"),
    LIMITS("Limits"),
    INSIGHTS("Insights"),
    SETTINGS("Settings")
}

@Composable
fun OrluneRoot(app: OrluneApplication) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedTab by rememberSaveable { mutableStateOf(OrluneTab.HOME.name) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var exportError by remember { mutableStateOf(false) }
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

    var usageAccessGranted by remember { mutableStateOf(UsageAccessPermission.isGranted(context)) }
    var overlayGranted by remember { mutableStateOf(OverlayPermission.isGranted(context)) }
    var notificationGranted by remember { mutableStateOf(NotificationPermission.isGranted(context)) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                usageAccessGranted = UsageAccessPermission.isGranted(context)
                overlayGranted = OverlayPermission.isGranted(context)
                notificationGranted = NotificationPermission.isGranted(context)
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
                        icon = { Text(destination.label.take(1)) },
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
                    onOpenUsageAccess = { context.startActivity(UsageAccessPermission.settingsIntent()) },
                    onRefresh = { scope.launch { app.usageRepository.processNewEvents() } },
                    onFocus = { selectedTab = OrluneTab.FOCUS.name }
                )
                OrluneTab.FOCUS -> FocusScreen(
                    modifier = Modifier.padding(padding),
                    apps = apps,
                    sessions = focusSessions,
                    usageAccessGranted = usageAccessGranted,
                    overlayGranted = overlayGranted,
                    onOpenOverlay = { context.startActivity(OverlayPermission.settingsIntent(context)) },
                    onStart = { minutes, packages ->
                        scope.launch {
                            app.focusSessionRepository.startSession(minutes, packages)
                            BlockingMonitorService.start(context)
                        }
                    },
                    onStop = { scope.launch { app.focusSessionRepository.cancelActiveSessions() } }
                )
                OrluneTab.LIMITS -> LimitsScreen(
                    modifier = Modifier.padding(padding),
                    apps = apps,
                    rules = rules,
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
                    apps = comparisonApps
                )
                OrluneTab.SETTINGS -> SettingsScreen(
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
                    onDeleteRequest = { showDeleteDialog = true }
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

    if (exportError) {
        AlertDialog(
            onDismissRequest = { exportError = false },
            title = { Text("Export unavailable") },
            text = { Text("Orlune could not create the local export file. Your stored data was not changed.") },
            confirmButton = { TextButton(onClick = { exportError = false }) { Text("OK") } }
        )
    }
}

@Composable
private fun HomeScreen(
    modifier: Modifier,
    todayUsage: List<AppDailyUsage>,
    totalToday: Long,
    activeRules: Int,
    activeFocus: FocusSessionEntity?,
    usageAccessGranted: Boolean,
    onOpenUsageAccess: () -> Unit,
    onRefresh: () -> Unit,
    onFocus: () -> Unit
) {
    LazyColumn(modifier = modifier.fillMaxSize().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Spacer(modifier = Modifier.height(20.dp))
            Text("Orlune", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text("Your time, seen clearly.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (!usageAccessGranted) {
            item {
                InfoCard("Usage Access is off", "Orlune needs this local Android permission to measure app usage.", "Open settings", onOpenUsageAccess)
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Today", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(formatDuration(totalToday), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.SemiBold)
                    Text("tracked on this device", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onRefresh) { Text("Refresh") }
                        Button(onClick = onFocus) { Text("Start focus") }
                    }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                MetricCard("Rules", activeRules.toString(), Modifier.weight(1f))
                MetricCard("Focus", if (activeFocus == null) "Ready" else "Active", Modifier.weight(1f))
            }
        }
        item { Text("Most used today", style = MaterialTheme.typography.titleLarge) }
        if (todayUsage.isEmpty()) {
            item { EmptyState("No usage recorded yet today.", "Refresh after using an app.") }
        } else {
            items(todayUsage.take(5), key = { it.packageName }) { UsageLine(it.label ?: it.packageName, it.totalUsageSeconds) }
        }
        item { Spacer(modifier = Modifier.height(20.dp)) }
    }
}

@Composable
private fun FocusScreen(
    modifier: Modifier,
    apps: List<AppEntity>,
    sessions: List<FocusSessionEntity>,
    usageAccessGranted: Boolean,
    overlayGranted: Boolean,
    onOpenOverlay: () -> Unit,
    onStart: (Int, List<String>) -> Unit,
    onStop: () -> Unit
) {
    var minutesText by rememberSaveable { mutableStateOf("25") }
    var selectedPackages by remember { mutableStateOf(emptySet<String>()) }
    var customPackage by rememberSaveable { mutableStateOf("") }
    val duration = minutesText.toIntOrNull()
    val packages = (selectedPackages + customPackage.split(",").map { it.trim() }.filter { it.isNotEmpty() }).toList()
    val activeOrScheduled = sessions.filter {
        val state = FocusSessionEngine.stateOf(it, System.currentTimeMillis())
        state == FocusSessionState.ACTIVE || state == FocusSessionState.SCHEDULED
    }

    LazyColumn(modifier = modifier.fillMaxSize().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Spacer(modifier = Modifier.height(20.dp))
            Text("Focus", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.SemiBold)
            Text("Choose what to pause and for how long.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (!usageAccessGranted) item { EmptyState("Usage Access is required.", "Enable it in Android Settings before starting a focus session.") }
        if (!overlayGranted) item { InfoCard("Blocking permission is off", "A focus session can only interrupt other apps when overlay permission is enabled.", "Open settings", onOpenOverlay) }
        item {
            OutlinedTextField(value = minutesText, onValueChange = { minutesText = it.filter(Char::isDigit).take(4) }, label = { Text("Duration in minutes") }, modifier = Modifier.fillMaxWidth())
        }
        item { Text("Apps to pause", style = MaterialTheme.typography.titleLarge) }
        if (apps.isEmpty()) item { EmptyState("No known apps yet.", "Use Usage Access and refresh Home first.") }
        items(apps.take(30), key = { it.packageName }) { app ->
            SelectableAppRow(app, app.packageName in selectedPackages) {
                selectedPackages = if (app.packageName in selectedPackages) selectedPackages - app.packageName else selectedPackages + app.packageName
            }
        }
        item {
            OutlinedTextField(value = customPackage, onValueChange = { customPackage = it }, label = { Text("Or enter package names, comma-separated") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { if (usageAccessGranted && overlayGranted && duration != null && duration > 0 && packages.isNotEmpty()) onStart(duration, packages) }, enabled = usageAccessGranted && overlayGranted && duration != null && duration > 0 && packages.isNotEmpty(), modifier = Modifier.fillMaxWidth()) { Text("Start focus session") }
        }
        if (activeOrScheduled.isNotEmpty()) {
            item { Text("Current sessions", style = MaterialTheme.typography.titleLarge) }
            items(activeOrScheduled, key = { it.id }) { session -> SessionLine(session) }
            item { OutlinedButton(onClick = onStop, modifier = Modifier.fillMaxWidth()) { Text("Stop active session") } }
        }
        item { Spacer(modifier = Modifier.height(20.dp)) }
    }
}

@Composable
private fun LimitsScreen(
    modifier: Modifier,
    apps: List<AppEntity>,
    rules: List<RuleEntity>,
    onAddLimit: (String, Long) -> Unit,
    onAddSchedule: (String, String, String, String, String) -> Unit,
    onDelete: (RuleEntity) -> Unit
) {
    var packageName by rememberSaveable { mutableStateOf("") }
    var minutes by rememberSaveable { mutableStateOf("30") }
    var scheduleName by rememberSaveable { mutableStateOf("") }
    var schedulePackage by rememberSaveable { mutableStateOf("") }
    var scheduleDays by rememberSaveable { mutableStateOf("MON,TUE,WED,THU,FRI") }
    var scheduleStart by rememberSaveable { mutableStateOf("22:00") }
    var scheduleEnd by rememberSaveable { mutableStateOf("07:00") }
    val knownNames = apps.map { it.packageName }
    val scheduleValid = isValidSchedule(scheduleDays, scheduleStart, scheduleEnd)
    LazyColumn(modifier = modifier.fillMaxSize().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Spacer(modifier = Modifier.height(20.dp))
            Text("Limits", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.SemiBold)
            Text("Simple rules, under your control.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            FormCard("Daily app limit") {
                OutlinedTextField(value = packageName, onValueChange = { packageName = it }, label = { Text("Package name") }, supportingText = { if (knownNames.isNotEmpty()) Text("Known: ${knownNames.take(3).joinToString()}") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = minutes, onValueChange = { minutes = it.filter(Char::isDigit).take(5) }, label = { Text("Minutes") }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("30", "45", "60", "90").forEach { value -> OutlinedButton(onClick = { minutes = value }) { Text("$value m") } } }
                Button(onClick = { val value = minutes.toLongOrNull(); if (!packageName.isBlank() && value != null && value > 0) { onAddLimit(packageName.trim(), value * 60); packageName = "" } }, modifier = Modifier.fillMaxWidth()) { Text("Add limit") }
            }
        }
        item {
            FormCard("Recurring schedule") {
                OutlinedTextField(value = scheduleName, onValueChange = { scheduleName = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = schedulePackage, onValueChange = { schedulePackage = it }, label = { Text("Package name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = scheduleDays, onValueChange = { scheduleDays = it }, label = { Text("Days, e.g. MON,WED") }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = scheduleStart, onValueChange = { scheduleStart = it }, label = { Text("Start") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = scheduleEnd, onValueChange = { scheduleEnd = it }, label = { Text("End") }, modifier = Modifier.weight(1f))
                }
                Button(onClick = { if (scheduleName.isNotBlank() && schedulePackage.isNotBlank() && scheduleValid) { onAddSchedule(scheduleName.trim(), schedulePackage.trim(), scheduleDays.trim().uppercase(), scheduleStart.trim(), scheduleEnd.trim()); scheduleName = ""; schedulePackage = "" } }, enabled = scheduleName.isNotBlank() && schedulePackage.isNotBlank() && scheduleValid, modifier = Modifier.fillMaxWidth()) { Text("Add schedule") }
            }
        }
        item { Text("Active rules", style = MaterialTheme.typography.titleLarge) }
        if (rules.isEmpty()) item { EmptyState("No rules yet.", "Add a limit or schedule above.") }
        items(rules, key = { it.id }) { rule ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(rule.targetPackageOrCategory, fontWeight = FontWeight.Medium)
                    Text(if (rule.type == "limit") "Daily limit · ${formatDuration(rule.threshold ?: 0)}" else "Scheduled restriction", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(onClick = { onDelete(rule) }) { Text("Remove") }
            }
        }
        item { Spacer(modifier = Modifier.height(20.dp)) }
    }
}

@Composable
private fun InsightsScreen(modifier: Modifier, lastWeekTotal: Long, previousWeekTotal: Long, apps: List<AppPeriodUsage>) {
    val change = lastWeekTotal - previousWeekTotal
    LazyColumn(modifier = modifier.fillMaxSize().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Spacer(modifier = Modifier.height(20.dp))
            Text("Insights", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.SemiBold)
            Text("Facts from your local history.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            FormCard("Seven-day comparison") {
                ComparisonLine("This week", formatDuration(lastWeekTotal))
                ComparisonLine("Last week", formatDuration(previousWeekTotal))
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                ComparisonLine("Change", signedDuration(change))
            }
        }
        item { Text("Apps in the last 14 days", style = MaterialTheme.typography.titleLarge) }
        if (apps.isEmpty()) item { EmptyState("Not enough local history yet.", "Keep using Orlune and refresh usage data.") }
        items(apps.take(10), key = { it.packageName }) { app -> UsageLine(app.label ?: app.packageName, app.totalUsageSeconds) }
        item { Spacer(modifier = Modifier.height(20.dp)) }
    }
}

@Composable
private fun SettingsScreen(
    modifier: Modifier,
    themeMode: String,
    usageAccessGranted: Boolean,
    overlayGranted: Boolean,
    notificationGranted: Boolean,
    onThemeChange: (String) -> Unit,
    onOpenUsageAccess: () -> Unit,
    onOpenOverlay: () -> Unit,
    onExport: () -> Unit,
    onDeleteRequest: () -> Unit
) {
    val context = LocalContext.current
    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LazyColumn(modifier = modifier.fillMaxSize().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Spacer(modifier = Modifier.height(20.dp))
            Text("Settings", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.SemiBold)
            Text("Private by design. Stored on this device.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            FormCard("Appearance") {
                listOf("system" to "System", "light" to "Light", "dark" to "Dark").forEach { (value, label) ->
                    Row(modifier = Modifier.fillMaxWidth().clickable { onThemeChange(value) }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = themeMode == value, onClick = { onThemeChange(value) })
                        Text(label)
                    }
                }
            }
        }
        item {
            FormCard("Permissions") {
                PermissionLine("Usage Access", usageAccessGranted, onOpenUsageAccess)
                PermissionLine("Display over other apps", overlayGranted, onOpenOverlay)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) PermissionLine("Notifications", notificationGranted) { notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }
            }
        }
        item {
            FormCard("Data") {
                Text("Usage, rules, schedules, focus sessions, and preferences stay local. Orlune has no account or server.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(onClick = onExport, modifier = Modifier.fillMaxWidth()) { Text("Export local data") }
                OutlinedButton(onClick = onDeleteRequest, modifier = Modifier.fillMaxWidth()) { Text("Delete all local data") }
            }
        }
        item {
            FormCard("About") {
                Text("Orlune", fontWeight = FontWeight.SemiBold)
                Text("Local digital wellbeing · version 0.1.0", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item { Spacer(modifier = Modifier.height(20.dp)) }
    }
}

@Composable
private fun InfoCard(title: String, body: String, action: String, onAction: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = onAction) { Text(action) }
        }
    }
}

@Composable
private fun MetricCard(label: String, value: String, modifier: Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(16.dp)) { Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(value, fontWeight = FontWeight.SemiBold) }
    }
}

@Composable
private fun FormCard(title: String, content: @Composable () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { Text(title, style = MaterialTheme.typography.titleLarge); content() }
    }
}

@Composable
private fun EmptyState(title: String, body: String) {
    Column(modifier = Modifier.padding(vertical = 12.dp)) { Text(title, fontWeight = FontWeight.Medium); Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant) }
}

@Composable
private fun UsageLine(label: String, seconds: Long) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp), horizontalArrangement = Arrangement.SpaceBetween) { Text(label, modifier = Modifier.weight(1f)); Text(formatDuration(seconds), color = MaterialTheme.colorScheme.onSurfaceVariant) }
}

@Composable
private fun SelectableAppRow(app: AppEntity, selected: Boolean, onToggle: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = selected, onCheckedChange = { onToggle() })
        Column { Text(app.label); Text(app.packageName, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) }
    }
}

@Composable
private fun SessionLine(session: FocusSessionEntity) {
    val state = FocusSessionEngine.stateOf(session, System.currentTimeMillis())
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(state.name.lowercase().replaceFirstChar { it.uppercase() }, fontWeight = FontWeight.Medium)
        Text("${formatDuration(session.completedMinutes * 60L)} of ${formatDuration(session.plannedMinutes * 60L)} · ${FocusSessionEngine.blockedPackages(session).joinToString()}", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun PermissionLine(label: String, granted: Boolean, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) { Text(label); Text(if (granted) "Granted" else "Not granted", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        if (!granted) TextButton(onClick = onClick) { Text("Open") }
    }
}

@Composable
private fun ComparisonLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(label); Text(value, fontWeight = FontWeight.Medium) }
}

private fun formatDuration(totalSeconds: Long): String {
    val safeSeconds = totalSeconds.coerceAtLeast(0)
    val totalMinutes = safeSeconds / 60
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}

private fun signedDuration(seconds: Long): String = if (seconds < 0) "−${formatDuration(-seconds)}" else "+${formatDuration(seconds)}"

private fun isValidSchedule(days: String, start: String, end: String): Boolean {
    if (days.isBlank()) return false
    return runCatching {
        com.orlune.app.core.domain.rules.ScheduleEngine.parse(
            ScheduleEntity(
                name = "validation",
                daysOfWeek = days.split(",").map { it.trim().uppercase() }.filter { it.isNotEmpty() }.joinToString(","),
                startTime = start,
                endTime = end,
                associatedRuleId = 0L
            )
        )
    }.isSuccess
}
