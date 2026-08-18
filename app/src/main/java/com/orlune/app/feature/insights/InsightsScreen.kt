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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.orlune.app.core.domain.insights.InsightsMetrics
import com.orlune.app.data.local.dao.AppDailyUsage
import com.orlune.app.data.local.dao.AppPeriodUsage
import com.orlune.app.data.local.dao.DayAppUsageRow
import com.orlune.app.data.local.entity.FocusSessionEntity
import com.orlune.app.data.local.entity.RuleEntity
import com.orlune.app.data.local.entity.SessionEntity
import com.orlune.app.platform.usage.InstalledAppSource
import com.orlune.app.ui.components.AppIcon
import com.orlune.app.ui.components.AppUsageRow
import com.orlune.app.ui.components.ComparisonLine
import com.orlune.app.ui.components.EmptyState
import com.orlune.app.ui.components.FormCard
import com.orlune.app.ui.components.WeekUsageBar
import com.orlune.app.ui.components.formatDuration
import com.orlune.app.ui.components.rememberAppDisplayInfos
import com.orlune.app.ui.components.signedDuration
import java.time.ZoneId

private enum class InsightsRange { SEVEN_DAYS, FOUR_WEEKS }

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
    insightsPeriodEndExclusiveMillis: Long,
    fourWeekDayRows: List<DayAppUsageRow>,
    today: Long,
    zoneId: ZoneId
) {
    var range by rememberSaveable { mutableStateOf(InsightsRange.SEVEN_DAYS) }
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

    // Four consecutive 7-day buckets, most recent first — computed regardless of which
    // range is currently displayed so the highlight line (which compares week 1 vs
    // week 2) is always available the instant the user switches to "4 weeks", with no
    // extra query round-trip.
    val weeks = remember(fourWeekDayRows, rules, focusSessions, today, zoneId) {
        InsightsMetrics.weeklyBreakdown(fourWeekDayRows, rules, focusSessions, today, weeks = 4, zoneId = zoneId)
    }
    val highlight = remember(weeks) { InsightsMetrics.highlight(weeks[0], weeks.getOrNull(1)) }
    val weekDisplayInfos = rememberAppDisplayInfos(
        installedAppSource,
        weeks.flatMap { week -> week.topApps.map { it.first to null as String? } }.distinct()
    )

    LazyColumn(modifier = modifier.fillMaxSize().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Spacer(modifier = Modifier.height(20.dp))
            Text("Insights", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.SemiBold)
            Text("Facts from your local history.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (range == InsightsRange.FOUR_WEEKS && highlight != null) {
            item { Text(highlight, style = MaterialTheme.typography.bodyMedium) }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = range == InsightsRange.SEVEN_DAYS,
                    onClick = { range = InsightsRange.SEVEN_DAYS },
                    label = { Text("7 days") }
                )
                FilterChip(
                    selected = range == InsightsRange.FOUR_WEEKS,
                    onClick = { range = InsightsRange.FOUR_WEEKS },
                    label = { Text("4 weeks") }
                )
            }
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

        if (range == InsightsRange.SEVEN_DAYS) {
            item { Text("Apps in the last 14 days", style = MaterialTheme.typography.titleLarge) }
            if (displayedApps.isEmpty()) item { EmptyState("Not enough local history yet.", "Keep using Orlune and refresh usage data.") }
            items(displayedApps, key = { it.packageName }) { app ->
                val display = displayInfos[app.packageName]
                if (display != null) AppUsageRow(display, app.totalUsageSeconds)
            }
        } else {
            item { Text("Weekly breakdown", style = MaterialTheme.typography.titleLarge) }
            val maxWeekSeconds = weeks.maxOfOrNull { it.totalUsageSeconds } ?: 0L
            items(weeks.size) { index ->
                val week = weeks[index]
                FormCard("Week ${index + 1}") {
                    ComparisonLine("Total usage", formatDuration(week.totalUsageSeconds))
                    WeekUsageBar(seconds = week.totalUsageSeconds, maxSeconds = maxWeekSeconds, modifier = Modifier.padding(vertical = 4.dp))
                    ComparisonLine("Focus time", formatDuration(week.focusSeconds))
                    if (week.ruleDays > 0) {
                        ComparisonLine("Limit compliance", "${week.compliantDays} of ${week.ruleDays} days")
                    }
                    if (week.topApps.isNotEmpty()) {
                        Text(
                            "Top apps",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        week.topApps.forEach { (packageName, seconds) ->
                            val display = weekDisplayInfos[packageName]
                            if (display != null) AppUsageRow(display, seconds)
                        }
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(20.dp)) }
    }
}
