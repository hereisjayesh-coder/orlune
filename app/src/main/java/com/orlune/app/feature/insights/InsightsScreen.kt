package com.orlune.app.feature.insights

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.orlune.app.core.domain.insights.InsightsMetrics
import com.orlune.app.data.local.dao.AppDailyUsage
import com.orlune.app.data.local.dao.AppPeriodUsage
import com.orlune.app.data.local.entity.FocusSessionEntity
import com.orlune.app.data.local.entity.RuleEntity
import com.orlune.app.data.local.entity.SessionEntity
import com.orlune.app.platform.usage.InstalledAppSource
import com.orlune.app.ui.components.AppIcon
import com.orlune.app.ui.components.AppUsageRow
import com.orlune.app.ui.components.ComparisonLine
import com.orlune.app.ui.components.EmptyState
import com.orlune.app.ui.components.FormCard
import com.orlune.app.ui.components.formatDuration
import com.orlune.app.ui.components.rememberAppDisplayInfos
import com.orlune.app.ui.components.signedDuration

@Composable
fun InsightsScreen(
    modifier: Modifier,
    lastWeekTotal: Long,
    previousWeekTotal: Long,
    apps: List<AppPeriodUsage>,
    installedAppSource: InstalledAppSource,
    ownPackageName: String,
    rules: List<RuleEntity>,
    todayUsage: List<AppDailyUsage>,
    focusSessions: List<FocusSessionEntity>,
    longestSession: SessionEntity?,
    insightsPeriodStartMillis: Long,
    insightsPeriodEndExclusiveMillis: Long
) {
    val change = lastWeekTotal - previousWeekTotal
    val displayedApps = remember(apps, ownPackageName) {
        apps.filter { it.packageName != ownPackageName }.take(10)
    }
    val displayInfos = rememberAppDisplayInfos(installedAppSource, displayedApps.map { it.packageName to it.label })

    val longestSessionEntries = remember(longestSession) {
        listOfNotNull(longestSession?.let { it.packageName to null as String? })
    }
    val longestSessionDisplayInfos = rememberAppDisplayInfos(installedAppSource, longestSessionEntries)
    val longestSessionDisplay = longestSession?.let { longestSessionDisplayInfos[it.packageName] }
    val longestSessionSeconds = longestSession?.let { (it.endTs!! - it.startTs) / 1000 }

    val focusSessionCount = remember(focusSessions, insightsPeriodStartMillis, insightsPeriodEndExclusiveMillis) {
        InsightsMetrics.focusSessionCount(focusSessions, insightsPeriodStartMillis, insightsPeriodEndExclusiveMillis)
    }
    val focusSeconds = remember(focusSessions, insightsPeriodStartMillis, insightsPeriodEndExclusiveMillis) {
        InsightsMetrics.focusSecondsTotal(focusSessions, insightsPeriodStartMillis, insightsPeriodEndExclusiveMillis)
    }

    val todaySecondsByPackage = remember(todayUsage) { todayUsage.associate { it.packageName to it.totalUsageSeconds } }
    val limitCompliance = remember(rules, todaySecondsByPackage) {
        InsightsMetrics.limitCompliance(rules, todaySecondsByPackage)
    }

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
        item {
            FormCard("Last 14 days at a glance") {
                if (longestSessionDisplay != null && longestSessionSeconds != null) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        AppIcon(longestSessionDisplay.icon)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Longest session", modifier = Modifier.weight(1f))
                        Text(
                            "${formatDuration(longestSessionSeconds)} · ${longestSessionDisplay.label}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    ComparisonLine("Longest session", "No sessions recorded yet")
                }
                ComparisonLine("Focus sessions", focusSessionCount.toString())
                ComparisonLine("Focus time", formatDuration(focusSeconds))
                if (limitCompliance.total > 0) {
                    ComparisonLine("Daily limits met today", "${limitCompliance.compliant} of ${limitCompliance.total}")
                }
            }
        }
        item { Text("Apps in the last 14 days", style = MaterialTheme.typography.titleLarge) }
        if (displayedApps.isEmpty()) item { EmptyState("Not enough local history yet.", "Keep using Orlune and refresh usage data.") }
        items(displayedApps, key = { it.packageName }) { app ->
            val display = displayInfos[app.packageName]
            if (display != null) AppUsageRow(display, app.totalUsageSeconds)
        }
        item { Spacer(modifier = Modifier.height(20.dp)) }
    }
}
