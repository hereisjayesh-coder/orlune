package com.orlune.app.feature.focus

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.orlune.app.core.domain.focus.FocusSessionEngine
import com.orlune.app.core.domain.focus.FocusSessionState
import com.orlune.app.data.local.entity.AppEntity
import com.orlune.app.data.local.entity.FocusSessionEntity
import com.orlune.app.ui.components.EmptyState
import com.orlune.app.ui.components.InfoCard
import com.orlune.app.ui.components.SelectableAppRow
import com.orlune.app.ui.components.SessionLine

@Composable
fun FocusScreen(
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
