package com.orlune.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
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
import com.orlune.app.data.local.dao.AppDailyUsage
import com.orlune.app.data.repository.UsageRepository
import com.orlune.app.platform.usage.UsageAccessPermission
import com.orlune.app.ui.theme.OrluneTheme
import kotlinx.coroutines.launch

/**
 * Temporary debug screen for Phase 3 (Usage Monitoring) — functional, not polished.
 * Verifies the permission flow and the read side of the pipeline end to end; the
 * real onboarding/home UI is Phase 8+.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val usageRepository = (application as OrluneApplication).usageRepository
        setContent {
            OrluneTheme {
                UsageDebugScreen(usageRepository)
            }
        }
    }
}

@Composable
private fun UsageDebugScreen(usageRepository: UsageRepository) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    var permissionGranted by remember { mutableStateOf(UsageAccessPermission.isGranted(context)) }
    var refreshing by remember { mutableStateOf(false) }

    // The user grants Usage Access in Settings, outside the app, so re-check on
    // every resume rather than only once at launch.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                permissionGranted = UsageAccessPermission.isGranted(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp)) {
            Text(text = "Orlune", style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(16.dp))

            if (!permissionGranted) {
                Text(
                    text = "Orlune needs Usage Access to see which apps you use and for " +
                        "how long. This stays on your device — nothing is ever uploaded."
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { context.startActivity(UsageAccessPermission.settingsIntent()) }) {
                    Text("Open Usage Access settings")
                }
            } else {
                Button(
                    onClick = {
                        scope.launch {
                            refreshing = true
                            usageRepository.processNewEvents()
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

                val todayUsage by usageRepository.observeTodayUsage().collectAsState(initial = emptyList())
                if (todayUsage.isEmpty()) {
                    Text("No usage recorded yet today. Use a few apps, then tap refresh.")
                } else {
                    LazyColumn {
                        items(todayUsage) { row -> UsageRow(row) }
                    }
                }
            }
        }
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
