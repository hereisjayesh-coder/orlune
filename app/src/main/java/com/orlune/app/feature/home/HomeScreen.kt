package com.orlune.app.feature.home

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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.orlune.app.data.local.dao.AppDailyUsage
import com.orlune.app.data.local.entity.FocusSessionEntity
import com.orlune.app.ui.components.EmptyState
import com.orlune.app.ui.components.InfoCard
import com.orlune.app.ui.components.MetricCard
import com.orlune.app.ui.components.UsageLine
import com.orlune.app.ui.components.formatDuration

@Composable
fun HomeScreen(
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
