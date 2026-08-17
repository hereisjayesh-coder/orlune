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
import com.orlune.app.core.domain.focus.FocusNotificationPolicy
import com.orlune.app.data.local.entity.FocusSessionEntity
import com.orlune.app.feature.apppicker.AppPickerMode
import com.orlune.app.feature.apppicker.AppPickerScreen
import com.orlune.app.platform.usage.InstalledApp
import com.orlune.app.platform.usage.InstalledAppSource

private sealed class FocusDestination {
    data object Root : FocusDestination()
    data object PickApps : FocusDestination()
    data object PickAllowedNotificationApps : FocusDestination()
}

/**
 * Owns Focus' app-picker sub-navigation and the in-progress session-setup state
 * (selected apps, duration, notification policy, allowed-notification apps) that must
 * survive a round trip to either picker — matches LimitsSection/SettingsSection's
 * manual-back-stack pattern. Reuses the same [AppPickerScreen] for both "apps to
 * pause" and "apps allowed to notify" — they're two independent selections, kept in
 * separate state so picking one never clobbers the other.
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
    notificationPolicyAccessGranted: Boolean,
    onOpenOverlay: () -> Unit,
    onOpenNotificationPolicySettings: () -> Unit,
    onStart: (
        minutes: Int,
        packages: List<String>,
        notificationPolicy: FocusNotificationPolicy,
        allowedNotificationPackages: List<String>
    ) -> Unit,
    onStop: () -> Unit
) {
    val backStack = remember { mutableStateListOf<FocusDestination>(FocusDestination.Root) }
    BackHandler(enabled = backStack.size > 1) { backStack.removeAt(backStack.lastIndex) }
    val saveableStateHolder = rememberSaveableStateHolder()

    var selectedApps by remember { mutableStateOf<List<InstalledApp>>(emptyList()) }
    var minutesText by rememberSaveable { mutableStateOf("25") }
    // Stored as the enum's name (a plain String), not the enum itself — this project
    // previously crashed from passing a non-Bundle-storable value into a Compose
    // saveable API (see docs/PROJECT_STATE.md's SaveableStateProvider incident); a
    // String is unambiguously safe, an enum's rememberSaveable support isn't worth the risk.
    var notificationPolicyName by rememberSaveable { mutableStateOf(FocusNotificationPolicy.ALLOW_ALL.name) }
    val notificationPolicy = FocusNotificationPolicy.fromStored(notificationPolicyName)
    var allowedNotificationApps by remember { mutableStateOf<List<InstalledApp>>(emptyList()) }

    when (backStack.last()) {
        FocusDestination.Root -> saveableStateHolder.SaveableStateProvider(FocusDestination.Root.toString()) {
            FocusScreen(
                modifier = modifier,
                selectedApps = selectedApps,
                minutesText = minutesText,
                onMinutesChange = { minutesText = it },
                notificationPolicy = notificationPolicy,
                onNotificationPolicyChange = { notificationPolicyName = it.name },
                notificationPolicyAccessGranted = notificationPolicyAccessGranted,
                onOpenNotificationPolicySettings = onOpenNotificationPolicySettings,
                allowedNotificationApps = allowedNotificationApps,
                onPickAllowedNotificationApps = { backStack.add(FocusDestination.PickAllowedNotificationApps) },
                onRemoveAllowedNotificationApp = { packageName ->
                    allowedNotificationApps = allowedNotificationApps.filterNot { it.packageName == packageName }
                },
                sessions = sessions,
                usageAccessGranted = usageAccessGranted,
                overlayGranted = overlayGranted,
                onOpenOverlay = onOpenOverlay,
                onPickApps = { backStack.add(FocusDestination.PickApps) },
                onRemoveApp = { packageName -> selectedApps = selectedApps.filterNot { it.packageName == packageName } },
                onStart = {
                    val minutes = minutesText.toIntOrNull()
                    if (minutes != null && minutes > 0 && selectedApps.isNotEmpty()) {
                        onStart(
                            minutes,
                            selectedApps.map { it.packageName },
                            notificationPolicy,
                            allowedNotificationApps.map { it.packageName }
                        )
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
        FocusDestination.PickAllowedNotificationApps -> saveableStateHolder.SaveableStateProvider(FocusDestination.PickAllowedNotificationApps.toString()) {
            AppPickerScreen(
                modifier = modifier,
                installedAppSource = installedAppSource,
                ownPackageName = ownPackageName,
                todayUsageSecondsByPackage = todayUsageSecondsByPackage,
                mode = AppPickerMode.Multi(
                    initialSelection = allowedNotificationApps.map { it.packageName }.toSet(),
                    onConfirm = { picked ->
                        allowedNotificationApps = picked.toList()
                        backStack.removeAt(backStack.lastIndex)
                    }
                ),
                onBack = { backStack.removeAt(backStack.lastIndex) }
            )
        }
    }
}
