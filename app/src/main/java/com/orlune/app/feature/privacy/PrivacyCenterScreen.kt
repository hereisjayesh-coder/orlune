package com.orlune.app.feature.privacy

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.orlune.app.ui.components.FormCard
import com.orlune.app.ui.components.PermissionLine

@Composable
fun PrivacyCenterScreen(
    modifier: Modifier,
    onBack: () -> Unit,
    sessionCount: Int,
    dailyUsageCount: Int,
    ruleCount: Int,
    focusSessionCount: Int,
    knownAppCount: Int,
    usageAccessGranted: Boolean,
    overlayGranted: Boolean,
    notificationGranted: Boolean,
    onOpenUsageAccess: () -> Unit,
    onOpenOverlay: () -> Unit,
    onExport: () -> Unit,
    onDeleteAllRequest: () -> Unit,
    onResetRequest: () -> Unit,
    onOpenLegalCenter: () -> Unit
) {
    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    LazyColumn(modifier = modifier.fillMaxSize().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Spacer(modifier = Modifier.height(20.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text("Privacy Center", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            }
            Text(
                "What Orlune stores, what it sends, and how to control both.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp, top = 4.dp)
            )
        }
        item {
            FormCard("Data stored on this device") {
                Text(
                    "Everything below lives only in Orlune's local database on this device. Counts update live.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                DataCountLine("Usage sessions recorded", sessionCount)
                DataCountLine("Daily usage summaries", dailyUsageCount)
                DataCountLine("Rules & schedules", ruleCount)
                DataCountLine("Focus sessions", focusSessionCount)
                DataCountLine("Apps Orlune has seen", knownAppCount)
                DataCountLine("Appearance preference", 1)
            }
        }
        item {
            FormCard("Data transmitted") {
                Text(
                    "Nothing. Orlune does not request Android's INTERNET permission and cannot make network requests — there is no server for this data to go to.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        item {
            FormCard("Permissions") {
                PermissionLine("Usage Access", usageAccessGranted, onOpenUsageAccess)
                PermissionLine("Display over other apps", overlayGranted, onOpenOverlay)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    PermissionLine("Notifications", notificationGranted) { notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }
                }
            }
        }
        item {
            FormCard("Data retention") {
                Text(
                    "Orlune keeps what it stores until you delete it or uninstall the app. There is no automatic expiry today.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        item {
            FormCard("Export data") {
                Text(
                    "Creates a complete JSON copy of everything Orlune has stored and hands it to Android's share sheet — you choose where it goes.",
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedButton(onClick = onExport, modifier = Modifier.fillMaxWidth()) { Text("Export local data") }
            }
        }
        item {
            FormCard("Delete all data") {
                Text(
                    "Immediately and permanently clears every table in Orlune's local database and stops any active monitoring.",
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedButton(onClick = onDeleteAllRequest, modifier = Modifier.fillMaxWidth()) { Text("Delete all local data") }
            }
        }
        item {
            FormCard("Reset application") {
                Text(
                    "Returns Orlune to its initial, freshly-installed state — same effect as deleting all data.",
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedButton(onClick = onResetRequest, modifier = Modifier.fillMaxWidth()) { Text("Reset application") }
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenLegalCenter).padding(vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Privacy & Legal documents", style = MaterialTheme.typography.bodyLarge)
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item { Spacer(modifier = Modifier.height(20.dp)) }
    }
}

@Composable
private fun DataCountLine(label: String, count: Int) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Text(count.toString(), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
