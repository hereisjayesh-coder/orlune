package com.orlune.app.feature.settings

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.orlune.app.ui.components.FormCard
import com.orlune.app.ui.components.PermissionLine

@Composable
fun SettingsScreen(
    modifier: Modifier,
    themeMode: String,
    usageAccessGranted: Boolean,
    overlayGranted: Boolean,
    notificationGranted: Boolean,
    onThemeChange: (String) -> Unit,
    onOpenUsageAccess: () -> Unit,
    onOpenOverlay: () -> Unit,
    onExport: () -> Unit,
    onDeleteRequest: () -> Unit,
    onOpenPrivacyCenter: () -> Unit
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
            Row(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenPrivacyCenter).padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Privacy & Legal", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
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
