package com.orlune.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.orlune.app.core.domain.focus.FocusSessionEngine
import com.orlune.app.core.domain.focus.FocusSessionState
import com.orlune.app.data.local.dao.AppDailyUsage
import com.orlune.app.data.local.entity.AppListEntryEntity
import com.orlune.app.data.local.entity.FocusSessionEntity
import com.orlune.app.data.local.entity.RuleEntity
import com.orlune.app.data.local.entity.ScheduleEntity
import com.orlune.app.platform.blocking.BlockingMonitorService
import com.orlune.app.platform.blocking.NotificationPermission
import com.orlune.app.platform.blocking.OverlayPermission
import com.orlune.app.platform.usage.UsageAccessPermission
import com.orlune.app.data.local.entity.ThemePreferenceEntity
import com.orlune.app.ui.OrluneRoot
import com.orlune.app.ui.theme.OrluneTheme
import kotlinx.coroutines.launch

/**
 * Temporary debug screen for Phase 3 (Usage Monitoring), Phase 5 (App Blocking), and
 * Phase 6 (Focus Sessions & Scheduling) — functional, not polished. Verifies the
 * permission flows and all three pipelines end to end, including minimal rule/
 * schedule/focus-session creation forms since no real UI exists until Phase 8 (the
 * approved design prototype). The real onboarding/home UI is Phase 8+.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as OrluneApplication
        setContent {
            val themePreference by app.database.themePreferenceDao()
                .observe()
                .collectAsState(initial = ThemePreferenceEntity(themeId = "system"))
            OrluneTheme(themeMode = themePreference?.themeId ?: "system") {
                OrluneRoot(app)
            }
        }
    }
}

@Composable
private fun DebugScreen(app: OrluneApplication) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    var usageAccessGranted by remember { mutableStateOf(UsageAccessPermission.isGranted(context)) }
    var overlayGranted by remember { mutableStateOf(OverlayPermission.isGranted(context)) }
    var notificationGranted by remember { mutableStateOf(NotificationPermission.isGranted(context)) }
    var refreshing by remember { mutableStateOf(false) }

    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        notificationGranted = granted
    }

    // Usage Access and overlay permission are both granted in Settings, outside the
    // app, so re-check on every resume rather than only once at launch.
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

    val rules by app.database.ruleDao().observeAll().collectAsState(initial = emptyList())
    val allowEntries by app.database.appListEntryDao().observeByType("allow").collectAsState(initial = emptyList())
    val todayUsage by app.usageRepository.observeTodayUsage().collectAsState(initial = emptyList())
    val focusSessions by app.focusSessionRepository.observeAll().collectAsState(initial = emptyList())

    Scaffold { innerPadding ->
        LazyColumn(modifier = Modifier.padding(innerPadding).padding(16.dp)) {
            item { Text(text = "Orlune", style = MaterialTheme.typography.bodyLarge) }
            item { Spacer(modifier = Modifier.height(16.dp)) }

            if (!usageAccessGranted) {
                item {
                    Text(
                        text = "Orlune needs Usage Access to see which apps you use and for " +
                            "how long. This stays on your device — nothing is ever uploaded."
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { context.startActivity(UsageAccessPermission.settingsIntent()) }) {
                        Text("Open Usage Access settings")
                    }
                }
            } else {
                item {
                    Button(
                        onClick = {
                            scope.launch {
                                refreshing = true
                                app.usageRepository.processNewEvents()
                                refreshing = false
                            }
                        },
                        enabled = !refreshing
                    ) {
                        Text(if (refreshing) "Refreshing…" else "Refresh usage data")
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = "Today's usage", style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (todayUsage.isEmpty()) {
                    item { Text("No usage recorded yet today. Use a few apps, then tap refresh.") }
                } else {
                    items(todayUsage) { row -> UsageRow(row) }
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = "App blocking (Phase 5)", style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                item {
                    PermissionRow("Display over other apps (required for the block screen)", overlayGranted) {
                        context.startActivity(OverlayPermission.settingsIntent(context))
                    }
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    item {
                        PermissionRow("Notifications (optional — service runs either way)", notificationGranted) {
                            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row {
                        Button(onClick = { BlockingMonitorService.start(context) }, enabled = overlayGranted) {
                            Text("Start monitoring")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = { BlockingMonitorService.stop(context) }) {
                            Text("Stop monitoring")
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                item {
                    AddLimitRuleForm { packageName, minutes ->
                        scope.launch {
                            app.database.ruleDao().upsert(
                                RuleEntity(
                                    type = "limit",
                                    targetPackageOrCategory = packageName,
                                    threshold = minutes * 60L,
                                    windowDefinition = null
                                )
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    AddScheduleRuleForm { name, packageName, days, start, end ->
                        scope.launch {
                            val ruleId = app.database.ruleDao().upsert(
                                RuleEntity(
                                    type = "schedule",
                                    targetPackageOrCategory = packageName,
                                    threshold = null,
                                    windowDefinition = null
                                )
                            )
                            app.database.scheduleDao().upsert(
                                ScheduleEntity(
                                    name = name,
                                    daysOfWeek = days,
                                    startTime = start,
                                    endTime = end,
                                    associatedRuleId = ruleId
                                )
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    AddAllowListForm { packageName ->
                        scope.launch {
                            app.database.appListEntryDao().upsert(AppListEntryEntity(packageName, "allow"))
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = "Rules (${rules.size})", style = MaterialTheme.typography.bodyLarge)
                }
                items(rules) { rule ->
                    RuleRow(rule) { scope.launch { app.database.ruleDao().delete(rule) } }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = "Allow list (${allowEntries.size})", style = MaterialTheme.typography.bodyLarge)
                }
                items(allowEntries) { entry ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(entry.packageName)
                        Button(onClick = { scope.launch { app.database.appListEntryDao().delete(entry) } }) { Text("Remove") }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = "Focus sessions (Phase 6)", style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                item {
                    StartFocusSessionForm(
                        onStartNow = { packages, minutes ->
                            scope.launch {
                                app.focusSessionRepository.startSession(plannedMinutes = minutes, blockedPackages = packages)
                                BlockingMonitorService.start(context)
                            }
                        },
                        onStartLater = { packages, minutes, delayMinutes ->
                            scope.launch {
                                val startAt = System.currentTimeMillis() + delayMinutes * 60_000L
                                app.focusSessionRepository.startSession(
                                    plannedMinutes = minutes,
                                    blockedPackages = packages,
                                    startAt = startAt
                                )
                                BlockingMonitorService.start(context)
                            }
                        }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { scope.launch { app.focusSessionRepository.cancelActiveSessions() } }) {
                        Text("Stop active session")
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = "Sessions (${focusSessions.size})", style = MaterialTheme.typography.bodyLarge)
                }
                items(focusSessions) { session -> FocusSessionRow(session) }
            }
        }
    }
}

@Composable
private fun PermissionRow(label: String, granted: Boolean, onGrantClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = "$label: ${if (granted) "granted" else "not granted"}")
        if (!granted) {
            Button(onClick = onGrantClick) { Text("Grant") }
        }
    }
}

@Composable
private fun AddLimitRuleForm(onAdd: (packageName: String, minutes: Long) -> Unit) {
    var packageName by remember { mutableStateOf("") }
    var minutesText by remember { mutableStateOf("") }

    Column {
        Text("Add daily limit rule")
        OutlinedTextField(value = packageName, onValueChange = { packageName = it }, label = { Text("Package name") })
        OutlinedTextField(value = minutesText, onValueChange = { minutesText = it }, label = { Text("Limit (minutes)") })
        Button(
            onClick = {
                val minutes = minutesText.toLongOrNull()
                if (packageName.isNotBlank() && minutes != null && minutes > 0) {
                    onAdd(packageName.trim(), minutes)
                    packageName = ""
                    minutesText = ""
                }
            }
        ) {
            Text("Add limit rule")
        }
    }
}

@Composable
private fun AddScheduleRuleForm(onAdd: (name: String, packageName: String, daysCsv: String, startTime: String, endTime: String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var packageName by remember { mutableStateOf("") }
    var days by remember { mutableStateOf("MON,TUE,WED,THU,FRI,SAT,SUN") }
    var startTime by remember { mutableStateOf("00:00") }
    var endTime by remember { mutableStateOf("23:59") }

    Column {
        Text("Add schedule rule (default: blocks all day, every day — narrow the window to test)")
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Schedule name") })
        OutlinedTextField(value = packageName, onValueChange = { packageName = it }, label = { Text("Package name") })
        OutlinedTextField(value = days, onValueChange = { days = it }, label = { Text("Days (e.g. MON,TUE)") })
        OutlinedTextField(value = startTime, onValueChange = { startTime = it }, label = { Text("Start (HH:mm)") })
        OutlinedTextField(value = endTime, onValueChange = { endTime = it }, label = { Text("End (HH:mm)") })
        Button(
            onClick = {
                if (name.isNotBlank() && packageName.isNotBlank() && days.isNotBlank()) {
                    onAdd(name.trim(), packageName.trim(), days.trim(), startTime.trim(), endTime.trim())
                    name = ""
                    packageName = ""
                }
            }
        ) {
            Text("Add schedule rule")
        }
    }
}

@Composable
private fun AddAllowListForm(onAdd: (packageName: String) -> Unit) {
    var packageName by remember { mutableStateOf("") }

    Column {
        Text("Add essential-app exemption (always allowed, overrides any rule)")
        OutlinedTextField(value = packageName, onValueChange = { packageName = it }, label = { Text("Package name") })
        Button(
            onClick = {
                if (packageName.isNotBlank()) {
                    onAdd(packageName.trim())
                    packageName = ""
                }
            }
        ) {
            Text("Add to allow list")
        }
    }
}

@Composable
private fun StartFocusSessionForm(
    onStartNow: (blockedPackages: List<String>, minutes: Int) -> Unit,
    onStartLater: (blockedPackages: List<String>, minutes: Int, delayMinutes: Int) -> Unit
) {
    var packagesText by remember { mutableStateOf("") }
    var minutesText by remember { mutableStateOf("") }
    var delayMinutesText by remember { mutableStateOf("") }

    fun parsedPackages() = packagesText.split(",").map { it.trim() }.filter { it.isNotEmpty() }

    Column {
        Text("Start a focus session (blocks the listed packages while active)")
        OutlinedTextField(
            value = packagesText,
            onValueChange = { packagesText = it },
            label = { Text("Blocked packages (comma-separated)") }
        )
        OutlinedTextField(value = minutesText, onValueChange = { minutesText = it }, label = { Text("Duration (minutes)") })
        Button(
            onClick = {
                val minutes = minutesText.toIntOrNull()
                val packages = parsedPackages()
                if (packages.isNotEmpty() && minutes != null && minutes > 0) {
                    onStartNow(packages, minutes)
                    packagesText = ""
                    minutesText = ""
                }
            }
        ) {
            Text("Start now")
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = delayMinutesText,
            onValueChange = { delayMinutesText = it },
            label = { Text("Or start in N minutes (one-time scheduled session)") }
        )
        Button(
            onClick = {
                val minutes = minutesText.toIntOrNull()
                val delayMinutes = delayMinutesText.toIntOrNull()
                val packages = parsedPackages()
                if (packages.isNotEmpty() && minutes != null && minutes > 0 && delayMinutes != null && delayMinutes > 0) {
                    onStartLater(packages, minutes, delayMinutes)
                    packagesText = ""
                    minutesText = ""
                    delayMinutesText = ""
                }
            }
        ) {
            Text("Schedule one-time session")
        }
    }
}

@Composable
private fun FocusSessionRow(session: FocusSessionEntity) {
    val state = FocusSessionEngine.stateOf(session, System.currentTimeMillis())
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            "$state: ${FocusSessionEngine.blockedPackages(session).joinToString(", ")} " +
                "(${session.completedMinutes}/${session.plannedMinutes}m)"
        )
    }
}

@Composable
private fun RuleRow(rule: RuleEntity, onDelete: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("${rule.type}: ${rule.targetPackageOrCategory}" + (rule.threshold?.let { " (${it / 60}m)" } ?: ""))
        Button(onClick = onDelete) { Text("Remove") }
    }
}

@Composable
private fun UsageRow(row: AppDailyUsage) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = row.label ?: row.packageName)
        Text(text = formatDuration(row.totalUsageSeconds))
    }
}

private fun formatDuration(totalSeconds: Long): String {
    val totalMinutes = totalSeconds / 60
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}
