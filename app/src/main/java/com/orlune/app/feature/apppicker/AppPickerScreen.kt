package com.orlune.app.feature.apppicker

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.orlune.app.platform.usage.InstalledApp
import com.orlune.app.platform.usage.InstalledAppSource
import com.orlune.app.ui.components.AppIcon
import com.orlune.app.ui.components.EmptyState
import com.orlune.app.ui.components.formatDuration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** How the picker behaves: pick exactly one app and return immediately (matches
 * RuleEntity's single target-package design), or select any number with an explicit
 * confirm step (matches a focus session's multi-app blockedPackages list). */
sealed class AppPickerMode {
    data class Single(val onPicked: (InstalledApp) -> Unit) : AppPickerMode()
    data class Multi(val initialSelection: Set<String>, val onConfirm: (Set<InstalledApp>) -> Unit) : AppPickerMode()
}

@Composable
fun AppPickerScreen(
    modifier: Modifier,
    installedAppSource: InstalledAppSource,
    ownPackageName: String,
    todayUsageSecondsByPackage: Map<String, Long>,
    mode: AppPickerMode,
    onBack: () -> Unit,
    title: String = "Select apps to limit",
    subtitle: String = "Choose the apps Orlune should help you control.",
    /** When set, shows an explicit "Skip" action alongside "Done" in [AppPickerMode.Multi]
     * — lets a caller (onboarding) proceed with zero apps selected without loosening
     * "Done"'s existing enabled-only-with-a-selection rule for every other caller. */
    onSkip: (() -> Unit)? = null
) {
    var apps by remember { mutableStateOf<List<InstalledApp>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var query by rememberSaveable { mutableStateOf("") }
    var selection by remember {
        mutableStateOf((mode as? AppPickerMode.Multi)?.initialSelection ?: emptySet())
    }

    LaunchedEffect(Unit) {
        apps = withContext(Dispatchers.Default) { installedAppSource.listLaunchableApps(ownPackageName) }
        loading = false
    }

    val onRowClick: (InstalledApp) -> Unit = { app ->
        when (mode) {
            is AppPickerMode.Single -> mode.onPicked(app)
            is AppPickerMode.Multi -> selection = if (app.packageName in selection) {
                selection - app.packageName
            } else {
                selection + app.packageName
            }
        }
    }

    val filtered = if (query.isBlank()) apps else apps.filter { it.label.contains(query, ignoreCase = true) }
    val frequentlyUsed = if (query.isBlank()) {
        apps.filter { (todayUsageSecondsByPackage[it.packageName] ?: 0L) > 0L }
            .sortedByDescending { todayUsageSecondsByPackage[it.packageName] }
            .take(5)
    } else emptyList()

    Column(modifier = modifier.fillMaxSize()) {
        Spacer(modifier = Modifier.height(20.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 8.dp)) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Spacer(modifier = Modifier.width(4.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Search apps...") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        if (mode is AppPickerMode.Multi) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (selection.isEmpty()) "No apps selected" else "${selection.size} selected",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (onSkip != null) {
                        TextButton(onClick = onSkip) { Text("Skip") }
                    }
                    Button(
                        onClick = { mode.onConfirm(apps.filter { it.packageName in selection }.toSet()) },
                        enabled = selection.isNotEmpty()
                    ) { Text("Done") }
                }
            }
        }

        when {
            loading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            apps.isEmpty() -> Box(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
                EmptyState("No apps found.", "Orlune couldn't find any other apps on this device.")
            }
            query.isBlank() && filtered.isEmpty() -> Box(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
                EmptyState("No apps found.", "Orlune couldn't find any other apps on this device.")
            }
            query.isNotBlank() && filtered.isEmpty() -> Box(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
                EmptyState("No matches.", "Try a different search term.")
            }
            else -> LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
                if (mode is AppPickerMode.Multi && selection.isNotEmpty() && query.isBlank()) {
                    item {
                        Text(
                            "Selected",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )
                    }
                    items(apps.filter { it.packageName in selection }, key = { "selected-${it.packageName}" }) { app ->
                        AppPickerRow(
                            app = app,
                            selected = true,
                            multiSelect = true,
                            todaySeconds = todayUsageSecondsByPackage[app.packageName],
                            onClick = { selection = selection - app.packageName }
                        )
                    }
                    item { HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(vertical = 8.dp)) }
                }
                if (frequentlyUsed.isNotEmpty()) {
                    item {
                        Text(
                            "Frequently used today",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )
                    }
                    items(frequentlyUsed, key = { "frequent-${it.packageName}" }) { app ->
                        AppPickerRow(
                            app = app,
                            selected = app.packageName in selection,
                            multiSelect = mode is AppPickerMode.Multi,
                            todaySeconds = todayUsageSecondsByPackage[app.packageName],
                            onClick = { onRowClick(app) }
                        )
                    }
                    item { HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(vertical = 8.dp)) }
                }
                item {
                    Text(
                        if (query.isBlank()) "All apps" else "Results",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                    )
                }
                items(filtered, key = { "all-${it.packageName}" }) { app ->
                    AppPickerRow(
                        app = app,
                        selected = app.packageName in selection,
                        multiSelect = mode is AppPickerMode.Multi,
                        todaySeconds = todayUsageSecondsByPackage[app.packageName],
                        onClick = { onRowClick(app) }
                    )
                }
                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun AppPickerRow(
    app: InstalledApp,
    selected: Boolean,
    multiSelect: Boolean,
    todaySeconds: Long?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppIcon(app.icon)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(app.label, style = MaterialTheme.typography.bodyLarge)
            if (todaySeconds != null && todaySeconds > 0) {
                Text(
                    "${formatDuration(todaySeconds)} today",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (multiSelect) {
            Checkbox(checked = selected, onCheckedChange = { onClick() })
        } else {
            RadioButton(selected = selected, onClick = onClick)
        }
    }
}
