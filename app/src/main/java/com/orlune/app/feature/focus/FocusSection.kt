package com.orlune.app.feature.focus

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.orlune.app.data.local.entity.FocusSessionEntity
import com.orlune.app.feature.apppicker.AppPickerMode
import com.orlune.app.feature.apppicker.AppPickerScreen
import com.orlune.app.platform.usage.InstalledApp
import com.orlune.app.platform.usage.InstalledAppSource

private sealed class FocusDestination {
    data object Root : FocusDestination()
    data object PickApps : FocusDestination()
}

/**
 * Owns Focus' app-picker sub-navigation and the in-progress session-setup state
 * (selected apps, duration) that must survive a round trip to the picker — matches
 * LimitsSection/SettingsSection's manual-back-stack pattern.
 */
@Composable
fun FocusSection(
    modifier: Modifier,
    installedAppSource: InstalledAppSource,
    ownPackageName: String,
    todayUsageSecondsByPackage: Map<String, Long>,
    sessions: List<FocusSessionEntity>,
    usageAccessGranted: Boolean,
    overlayGranted: Boolean,
    onOpenOverlay: () -> Unit,
    onStart: (minutes: Int, packages: List<String>) -> Unit,
    onStop: () -> Unit
) {
    val backStack = remember { mutableStateListOf<FocusDestination>(FocusDestination.Root) }
    BackHandler(enabled = backStack.size > 1) { backStack.removeAt(backStack.lastIndex) }
    val saveableStateHolder = rememberSaveableStateHolder()

    var selectedApps by remember { mutableStateOf<List<InstalledApp>>(emptyList()) }
    var minutesText by rememberSaveable { mutableStateOf("25") }

    when (backStack.last()) {
        FocusDestination.Root -> saveableStateHolder.SaveableStateProvider(FocusDestination.Root.toString()) {
            FocusScreen(
                modifier = modifier,
                selectedApps = selectedApps,
                minutesText = minutesText,
                onMinutesChange = { minutesText = it },
                sessions = sessions,
                usageAccessGranted = usageAccessGranted,
                overlayGranted = overlayGranted,
                onOpenOverlay = onOpenOverlay,
                onPickApps = { backStack.add(FocusDestination.PickApps) },
                onRemoveApp = { packageName -> selectedApps = selectedApps.filterNot { it.packageName == packageName } },
                onStart = {
                    val minutes = minutesText.toIntOrNull()
                    if (minutes != null && minutes > 0 && selectedApps.isNotEmpty()) {
                        onStart(minutes, selectedApps.map { it.packageName })
                    }
                },
                onStop = onStop
            )
        }
        FocusDestination.PickApps -> saveableStateHolder.SaveableStateProvider(FocusDestination.PickApps.toString()) {
            AppPickerScreen(
                modifier = modifier,
                installedAppSource = installedAppSource,
                ownPackageName = ownPackageName,
                todayUsageSecondsByPackage = todayUsageSecondsByPackage,
                mode = AppPickerMode.Multi(
                    initialSelection = selectedApps.map { it.packageName }.toSet(),
                    onConfirm = { picked ->
                        selectedApps = picked.toList()
                        backStack.removeAt(backStack.lastIndex)
                    }
                ),
                onBack = { backStack.removeAt(backStack.lastIndex) }
            )
        }
    }
}
