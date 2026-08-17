package com.orlune.app.feature.insights

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.orlune.app.data.local.dao.AppPeriodUsage
import com.orlune.app.ui.components.ComparisonLine
import com.orlune.app.ui.components.EmptyState
import com.orlune.app.ui.components.FormCard
import com.orlune.app.ui.components.UsageLine
import com.orlune.app.ui.components.formatDuration
import com.orlune.app.ui.components.signedDuration

@Composable
fun InsightsScreen(modifier: Modifier, lastWeekTotal: Long, previousWeekTotal: Long, apps: List<AppPeriodUsage>) {
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
