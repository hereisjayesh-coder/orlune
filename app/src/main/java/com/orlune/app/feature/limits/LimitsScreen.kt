package com.orlune.app.feature.limits

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.orlune.app.core.domain.rules.DailyLimitInput
import com.orlune.app.data.local.entity.AppEntity
import com.orlune.app.data.local.entity.RuleEntity
import com.orlune.app.platform.usage.InstalledAppSource
import com.orlune.app.ui.components.AppIcon
import com.orlune.app.ui.components.DurationStepper
import com.orlune.app.ui.components.EmptyState
import com.orlune.app.ui.components.FormCard
import com.orlune.app.ui.components.TimePickerField
import com.orlune.app.ui.components.WEEKDAY_ORDER
import com.orlune.app.ui.components.WeekdaySelector
import com.orlune.app.ui.components.formatDuration
import com.orlune.app.ui.components.isValidSchedule
import com.orlune.app.ui.components.rememberAppDisplayInfos

private val presetMinutes = listOf(30, 45, 60, 90)

@Composable
fun LimitsScreen(
    modifier: Modifier,
    apps: List<AppEntity>,
    rules: List<RuleEntity>,
    installedAppSource: InstalledAppSource,
    limitAppLabel: String?,
    onPickLimitApp: () -> Unit,
    scheduleAppLabel: String?,
    onPickScheduleApp: () -> Unit,
    onAddLimit: (Long) -> Unit,
    onAddSchedule: (String, String, String, String) -> Unit,
    onDelete: (RuleEntity) -> Unit
) {
    var selectedPresetMinutes by rememberSaveable { mutableStateOf<Int?>(30) }
    var customSelected by rememberSaveable { mutableStateOf(false) }
    var customHours by rememberSaveable { mutableStateOf(0) }
    var customMinutes by rememberSaveable { mutableStateOf(0) }
    var limitError by rememberSaveable { mutableStateOf<String?>(null) }
    var scheduleName by rememberSaveable { mutableStateOf("") }
    var scheduleDays by rememberSaveable { mutableStateOf("MON,TUE,WED,THU,FRI") }
    var scheduleStart by rememberSaveable { mutableStateOf("22:00") }
    var scheduleEnd by rememberSaveable { mutableStateOf("07:00") }
    val scheduleValid = isValidSchedule(scheduleDays, scheduleStart, scheduleEnd)
    val labelsByPackage = remember(apps) { apps.associate { it.packageName to it.label } }
    val ruleDisplayInfos = rememberAppDisplayInfos(
        installedAppSource,
        rules.map { it.targetPackageOrCategory to labelsByPackage[it.targetPackageOrCategory] }
    )

    LazyColumn(modifier = modifier.fillMaxSize().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Spacer(modifier = Modifier.height(20.dp))
            Text("Limits", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.SemiBold)
            Text("Simple rules, under your control.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            // Presets are shortcuts into the same (hours, minutes) -> seconds path as
            // Custom — DailyLimitInput.toThresholdSeconds validates both identically,
            // so a preset is never "more trusted" than a hand-typed custom duration.
            // Presets are flat minute counts (e.g. 90), so they're decomposed into
            // hours+minutes before validation, same as toThresholdSeconds expects.
            val currentHours = if (customSelected) customHours else (selectedPresetMinutes ?: 0) / 60
            val currentMinutes = if (customSelected) customMinutes else (selectedPresetMinutes ?: 0) % 60
            val previewSeconds = (currentHours * 3600L) + (currentMinutes * 60L)
            FormCard("Daily app limit") {
                AppPickerField(label = limitAppLabel, onClick = onPickLimitApp)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    presetMinutes.forEach { preset ->
                        FilterChip(
                            selected = !customSelected && selectedPresetMinutes == preset,
                            onClick = { selectedPresetMinutes = preset; customSelected = false; limitError = null },
                            label = { Text("${preset}m") }
                        )
                    }
                    FilterChip(
                        selected = customSelected,
                        onClick = { customSelected = true; limitError = null },
                        label = { Text("Custom") }
                    )
                }
                if (customSelected) {
                    DurationStepper(
                        hours = customHours,
                        minutes = customMinutes,
                        onHoursChange = { customHours = it; limitError = null },
                        onMinutesChange = { customMinutes = it; limitError = null },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Text("= ${formatDuration(previewSeconds)}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (limitError != null) {
                    Text(limitError.orEmpty(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
                Button(
                    onClick = {
                        if (limitAppLabel == null) {
                            limitError = "Choose an app first"
                            return@Button
                        }
                        DailyLimitInput.toThresholdSeconds(currentHours, currentMinutes).fold(
                            onSuccess = { seconds ->
                                onAddLimit(seconds)
                                limitError = null
                            },
                            onFailure = { limitError = it.message }
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Add limit") }
            }
        }
        item {
            FormCard("Recurring schedule") {
                OutlinedTextField(value = scheduleName, onValueChange = { scheduleName = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
                AppPickerField(label = scheduleAppLabel, onClick = onPickScheduleApp)
                Text("Days", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                val selectedDays = remember(scheduleDays) { scheduleDays.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet() }
                WeekdaySelector(
                    selectedDays = selectedDays,
                    onDaysChange = { days -> scheduleDays = WEEKDAY_ORDER.filter { it in days }.joinToString(",") }
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TimePickerField("Start", scheduleStart, { scheduleStart = it }, modifier = Modifier.weight(1f))
                    TimePickerField("End", scheduleEnd, { scheduleEnd = it }, modifier = Modifier.weight(1f))
                }
                val scheduleReady = scheduleName.isNotBlank() && scheduleAppLabel != null && scheduleValid
                Button(
                    onClick = {
                        if (scheduleReady) {
                            onAddSchedule(scheduleName.trim(), scheduleDays.trim().uppercase(), scheduleStart.trim(), scheduleEnd.trim())
                            scheduleName = ""
                        }
                    },
                    enabled = scheduleReady,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Add schedule") }
            }
        }
        item { Text("Active rules", style = MaterialTheme.typography.titleLarge) }
        if (rules.isEmpty()) item { EmptyState("No rules yet.", "Add a limit or schedule above.") }
        items(rules, key = { it.id }) { rule ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    AppIcon(ruleDisplayInfos[rule.targetPackageOrCategory]?.icon)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(ruleDisplayInfos[rule.targetPackageOrCategory]?.label ?: "Unknown app", fontWeight = FontWeight.Medium)
                        Text(if (rule.type == "limit") "Daily limit · ${formatDuration(rule.threshold ?: 0)}" else "Scheduled restriction", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                TextButton(onClick = { onDelete(rule) }) { Text("Remove") }
            }
        }
        item { Spacer(modifier = Modifier.height(20.dp)) }
    }
}

@Composable
private fun AppPickerField(label: String?, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label ?: "Choose an app",
            style = MaterialTheme.typography.bodyLarge,
            color = if (label != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
