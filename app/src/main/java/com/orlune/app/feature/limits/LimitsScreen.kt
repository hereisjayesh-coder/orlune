package com.orlune.app.feature.limits

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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.orlune.app.data.local.entity.AppEntity
import com.orlune.app.data.local.entity.RuleEntity
import com.orlune.app.ui.components.EmptyState
import com.orlune.app.ui.components.FormCard
import com.orlune.app.ui.components.formatDuration
import com.orlune.app.ui.components.isValidSchedule

@Composable
fun LimitsScreen(
    modifier: Modifier,
    apps: List<AppEntity>,
    rules: List<RuleEntity>,
    onAddLimit: (String, Long) -> Unit,
    onAddSchedule: (String, String, String, String, String) -> Unit,
    onDelete: (RuleEntity) -> Unit
) {
    var packageName by rememberSaveable { mutableStateOf("") }
    var minutes by rememberSaveable { mutableStateOf("30") }
    var scheduleName by rememberSaveable { mutableStateOf("") }
    var schedulePackage by rememberSaveable { mutableStateOf("") }
    var scheduleDays by rememberSaveable { mutableStateOf("MON,TUE,WED,THU,FRI") }
    var scheduleStart by rememberSaveable { mutableStateOf("22:00") }
    var scheduleEnd by rememberSaveable { mutableStateOf("07:00") }
    val knownNames = apps.map { it.packageName }
    val scheduleValid = isValidSchedule(scheduleDays, scheduleStart, scheduleEnd)
    LazyColumn(modifier = modifier.fillMaxSize().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Spacer(modifier = Modifier.height(20.dp))
            Text("Limits", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.SemiBold)
            Text("Simple rules, under your control.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            FormCard("Daily app limit") {
                OutlinedTextField(value = packageName, onValueChange = { packageName = it }, label = { Text("Package name") }, supportingText = { if (knownNames.isNotEmpty()) Text("Known: ${knownNames.take(3).joinToString()}") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = minutes, onValueChange = { minutes = it.filter(Char::isDigit).take(5) }, label = { Text("Minutes") }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("30", "45", "60", "90").forEach { value -> OutlinedButton(onClick = { minutes = value }) { Text("$value m") } } }
                Button(onClick = { val value = minutes.toLongOrNull(); if (!packageName.isBlank() && value != null && value > 0) { onAddLimit(packageName.trim(), value * 60); packageName = "" } }, modifier = Modifier.fillMaxWidth()) { Text("Add limit") }
            }
        }
        item {
            FormCard("Recurring schedule") {
                OutlinedTextField(value = scheduleName, onValueChange = { scheduleName = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = schedulePackage, onValueChange = { schedulePackage = it }, label = { Text("Package name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = scheduleDays, onValueChange = { scheduleDays = it }, label = { Text("Days, e.g. MON,WED") }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = scheduleStart, onValueChange = { scheduleStart = it }, label = { Text("Start") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = scheduleEnd, onValueChange = { scheduleEnd = it }, label = { Text("End") }, modifier = Modifier.weight(1f))
                }
                Button(onClick = { if (scheduleName.isNotBlank() && schedulePackage.isNotBlank() && scheduleValid) { onAddSchedule(scheduleName.trim(), schedulePackage.trim(), scheduleDays.trim().uppercase(), scheduleStart.trim(), scheduleEnd.trim()); scheduleName = ""; schedulePackage = "" } }, enabled = scheduleName.isNotBlank() && schedulePackage.isNotBlank() && scheduleValid, modifier = Modifier.fillMaxWidth()) { Text("Add schedule") }
            }
        }
        item { Text("Active rules", style = MaterialTheme.typography.titleLarge) }
        if (rules.isEmpty()) item { EmptyState("No rules yet.", "Add a limit or schedule above.") }
        items(rules, key = { it.id }) { rule ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(rule.targetPackageOrCategory, fontWeight = FontWeight.Medium)
                    Text(if (rule.type == "limit") "Daily limit · ${formatDuration(rule.threshold ?: 0)}" else "Scheduled restriction", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(onClick = { onDelete(rule) }) { Text("Remove") }
            }
        }
        item { Spacer(modifier = Modifier.height(20.dp)) }
    }
}
