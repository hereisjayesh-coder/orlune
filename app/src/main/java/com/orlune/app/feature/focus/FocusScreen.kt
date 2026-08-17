package com.orlune.app.feature.focus

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.orlune.app.core.domain.focus.FocusSessionEngine
import com.orlune.app.core.domain.focus.FocusSessionState
import com.orlune.app.data.local.entity.FocusSessionEntity
import com.orlune.app.platform.usage.InstalledApp
import com.orlune.app.ui.components.EmptyState
import com.orlune.app.ui.components.InfoCard
import com.orlune.app.ui.components.SessionLine

@Composable
fun FocusScreen(
    modifier: Modifier,
    selectedApps: List<InstalledApp>,
    minutesText: String,
    onMinutesChange: (String) -> Unit,
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

    LazyColumn(modifier = modifier.fillMaxSize().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Spacer(modifier = Modifier.height(20.dp))
            Text("Focus", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.SemiBold)
            Text("Choose what to pause and for how long.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (!usageAccessGranted) item { EmptyState("Usage Access is required.", "Enable it in Android Settings before starting a focus session.") }
        if (!overlayGranted) item { InfoCard("Blocking permission is off", "A focus session can only interrupt other apps when overlay permission is enabled.", "Open settings", onOpenOverlay) }
        item {
            OutlinedTextField(
                value = minutesText,
                onValueChange = { onMinutesChange(it.filter(Char::isDigit).take(4)) },
                label = { Text("Duration in minutes") },
                modifier = Modifier.fillMaxWidth()
            )
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
            Button(onClick = onStart, enabled = canStart, modifier = Modifier.fillMaxWidth()) { Text("Start focus session") }
        }
        if (activeOrScheduled.isNotEmpty()) {
            item { Text("Current sessions", style = MaterialTheme.typography.titleLarge) }
            items(activeOrScheduled, key = { it.id }) { session -> SessionLine(session) }
            item { OutlinedButton(onClick = onStop, modifier = Modifier.fillMaxWidth()) { Text("Stop active session") } }
        }
        item { Spacer(modifier = Modifier.height(20.dp)) }
    }
}
