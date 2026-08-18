package com.orlune.app.feature.focus

import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.orlune.app.core.domain.focus.FocusNotificationPolicy
import com.orlune.app.core.domain.focus.FocusSessionEngine
import com.orlune.app.core.domain.focus.FocusSessionState
import com.orlune.app.core.domain.focus.allowedNotificationPackages
import com.orlune.app.data.local.entity.FocusSessionEntity
import com.orlune.app.platform.usage.InstalledApp
import com.orlune.app.platform.usage.InstalledAppSource
import com.orlune.app.ui.components.DurationPresetSelector
import com.orlune.app.ui.components.EmptyState
import com.orlune.app.ui.components.InfoCard
import com.orlune.app.ui.components.NotificationPolicySelector
import com.orlune.app.ui.components.SessionLine
import com.orlune.app.ui.components.label
import com.orlune.app.ui.components.rememberAppDisplayInfos

@Composable
fun FocusScreen(
    modifier: Modifier,
    installedAppSource: InstalledAppSource,
    selectedApps: List<InstalledApp>,
    minutesText: String,
    onMinutesChange: (String) -> Unit,
    notificationPolicy: FocusNotificationPolicy,
    onNotificationPolicyChange: (FocusNotificationPolicy) -> Unit,
    notificationPolicyAccessGranted: Boolean,
    onOpenNotificationPolicySettings: () -> Unit,
    allowedNotificationApps: List<InstalledApp>,
    onPickAllowedNotificationApps: () -> Unit,
    onRemoveAllowedNotificationApp: (String) -> Unit,
    sessions: List<FocusSessionEntity>,
    usageAccessGranted: Boolean,
    overlayGranted: Boolean,
    onOpenOverlay: () -> Unit,
    onPickApps: () -> Unit,
    onRemoveApp: (String) -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit
) {
    val duration = minutesText.toIntOrNull()
    val canStart = usageAccessGranted && overlayGranted && duration != null && duration > 0 && selectedApps.isNotEmpty()
    val activeOrScheduled = sessions.filter {
        val state = FocusSessionEngine.stateOf(it, System.currentTimeMillis())
        state == FocusSessionState.ACTIVE || state == FocusSessionState.SCHEDULED
    }
    val blockedPackageEntries = remember(activeOrScheduled) {
        activeOrScheduled.flatMap { FocusSessionEngine.blockedPackages(it) }.distinct().map { it to (null as String?) }
    }
    val blockedAppDisplay = rememberAppDisplayInfos(installedAppSource, blockedPackageEntries)
    val involvesCalls = notificationPolicy == FocusNotificationPolicy.ALLOW_CALLS ||
        notificationPolicy == FocusNotificationPolicy.ALLOW_CALLS_AND_SELECTED

    LazyColumn(modifier = modifier.fillMaxSize().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Spacer(modifier = Modifier.height(20.dp))
            Text("ORLUNE", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Focus", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.SemiBold)
            Text("Choose what to pause and for how long.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (!usageAccessGranted) item { EmptyState("Usage Access is required.", "Enable it in Android Settings before starting a focus session.") }
        if (!overlayGranted) item { InfoCard("Blocking permission is off", "A focus session can only interrupt other apps when overlay permission is enabled.", "Open settings", onOpenOverlay) }

        item { Text("Focus duration", style = MaterialTheme.typography.titleLarge) }
        item { DurationPresetSelector(minutesText = minutesText, onMinutesChange = onMinutesChange) }
        item {
            OutlinedTextField(
                value = minutesText,
                onValueChange = { onMinutesChange(it.filter(Char::isDigit).take(4)) },
                label = { Text("Custom duration (minutes)") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        item { Text("Notification interruptions", style = MaterialTheme.typography.titleLarge) }
        item { NotificationPolicySelector(selected = notificationPolicy, onSelect = onNotificationPolicyChange) }
        if (notificationPolicy != FocusNotificationPolicy.ALLOW_ALL && !notificationPolicyAccessGranted) {
            item {
                InfoCard(
                    "Notification access needed",
                    "Focus can silence interruptions while you work. Orlune does not read or store your notification content.",
                    "Open notification settings",
                    onOpenNotificationPolicySettings
                )
            }
        }
        if (involvesCalls) {
            item {
                Text(
                    "Whether calls actually ring depends on Android and your device's own settings — Orlune cannot guarantee emergency calls get through on every device.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (notificationPolicy == FocusNotificationPolicy.ALLOW_CALLS_AND_SELECTED) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                item {
                    Text(
                        "Selecting individual apps isn't supported on this Android version — Focus will allow calls only.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            item {
                Text(
                    "Orlune can't mark another app's notifications as priority for you — pick apps below, then also mark each one \"Allow interruptions\" in its own Android notification settings.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().clickable(onClick = onPickAllowedNotificationApps).padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (allowedNotificationApps.isEmpty()) "Choose allowed apps" else "${allowedNotificationApps.size} apps selected — tap to change",
                        color = if (allowedNotificationApps.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                    )
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            items(allowedNotificationApps, key = { "allowed-${it.packageName}" }) { app ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(app.label, style = MaterialTheme.typography.bodyMedium)
                    IconButton(onClick = { onRemoveAllowedNotificationApp(app.packageName) }) {
                        Icon(Icons.Filled.Close, contentDescription = "Remove ${app.label}", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        item { Text("Apps to pause", style = MaterialTheme.typography.titleLarge) }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onPickApps)
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (selectedApps.isEmpty()) "Choose apps to pause" else "${selectedApps.size} apps selected — tap to change",
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (selectedApps.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                )
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        items(selectedApps, key = { it.packageName }) { app ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(app.label, style = MaterialTheme.typography.bodyMedium)
                IconButton(onClick = { onRemoveApp(app.packageName) }) {
                    Icon(Icons.Filled.Close, contentDescription = "Remove ${app.label}", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item {
            Button(onClick = onStart, enabled = canStart, modifier = Modifier.fillMaxWidth()) { Text("Start Focus") }
        }
        if (activeOrScheduled.isNotEmpty()) {
            item { Text("Current sessions", style = MaterialTheme.typography.titleLarge) }
            items(activeOrScheduled, key = { it.id }) { session ->
                SessionLine(session, blockedAppDisplay)
                Text(
                    notificationSummary(session, notificationPolicyAccessGranted),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            item { OutlinedButton(onClick = onStop, modifier = Modifier.fillMaxWidth()) { Text("Stop Focus") } }
        }
        item { Spacer(modifier = Modifier.height(20.dp)) }
    }
}

private fun notificationSummary(session: FocusSessionEntity, notificationPolicyAccessGranted: Boolean): String {
    val policy = FocusNotificationPolicy.fromStored(session.notificationPolicy)
    val base = when (policy) {
        FocusNotificationPolicy.ALLOW_ALL -> "Notifications: allowed as normal"
        FocusNotificationPolicy.SILENCE_ALL -> "Notifications: silenced"
        FocusNotificationPolicy.ALLOW_CALLS -> "Notifications: calls allowed, rest silenced"
        FocusNotificationPolicy.ALLOW_CALLS_AND_SELECTED ->
            "Notifications: calls + ${session.allowedNotificationPackages().size} app(s) allowed"
    }
    return if (policy != FocusNotificationPolicy.ALLOW_ALL && !notificationPolicyAccessGranted) {
        "$base — not applied (grant notification access in Settings)"
    } else {
        base
    }
}
