package com.orlune.app.feature.limits

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
import com.orlune.app.data.local.entity.AppEntity
import com.orlune.app.data.local.entity.RuleEntity
import com.orlune.app.feature.apppicker.AppPickerMode
import com.orlune.app.feature.apppicker.AppPickerScreen
import com.orlune.app.platform.usage.InstalledAppSource

private sealed class LimitsDestination {
    data object Root : LimitsDestination()
    data object PickAppForLimit : LimitsDestination()
    data object PickAppForSchedule : LimitsDestination()
}

/**
 * Owns Limits' app-picker sub-navigation, matching the manual-back-stack pattern
 * already established by SettingsSection — no navigation-compose dependency for
 * what's still a small, linear stack.
 */
@Composable
fun LimitsSection(
    modifier: Modifier,
    apps: List<AppEntity>,
    rules: List<RuleEntity>,
    installedAppSource: InstalledAppSource,
    ownPackageName: String,
    todayUsageSecondsByPackage: Map<String, Long>,
    onAddLimit: (packageName: String, thresholdSeconds: Long) -> Unit,
    onAddSchedule: (name: String, packageName: String, days: String, start: String, end: String) -> Unit,
    onDelete: (RuleEntity) -> Unit
) {
    val backStack = remember { mutableStateListOf<LimitsDestination>(LimitsDestination.Root) }
    BackHandler(enabled = backStack.size > 1) { backStack.removeAt(backStack.lastIndex) }
    // Retains each destination's own rememberSaveable state (typed schedule name/
    // days/times, duration presets) across a round trip to the app picker and back —
    // a plain `when` disposes that state on removal, which would otherwise silently
    // discard whatever the user had already typed before choosing an app.
    val saveableStateHolder = rememberSaveableStateHolder()

    var limitPackage by rememberSaveable { mutableStateOf<String?>(null) }
    var limitLabel by rememberSaveable { mutableStateOf<String?>(null) }
    var schedulePackage by rememberSaveable { mutableStateOf<String?>(null) }
    var scheduleLabel by rememberSaveable { mutableStateOf<String?>(null) }

    when (backStack.last()) {
        LimitsDestination.Root -> saveableStateHolder.SaveableStateProvider(LimitsDestination.Root.toString()) {
            LimitsScreen(
                modifier = modifier,
                apps = apps,
                rules = rules,
                installedAppSource = installedAppSource,
                limitAppLabel = limitLabel,
                onPickLimitApp = { backStack.add(LimitsDestination.PickAppForLimit) },
                scheduleAppLabel = scheduleLabel,
                onPickScheduleApp = { backStack.add(LimitsDestination.PickAppForSchedule) },
                onAddLimit = { seconds ->
                    limitPackage?.let { pkg ->
                        onAddLimit(pkg, seconds)
                        limitPackage = null
                        limitLabel = null
                    }
                },
                onAddSchedule = { name, days, start, end ->
                    schedulePackage?.let { pkg ->
                        onAddSchedule(name, pkg, days, start, end)
                        schedulePackage = null
                        scheduleLabel = null
                    }
                },
                onDelete = onDelete
            )
        }
        LimitsDestination.PickAppForLimit -> saveableStateHolder.SaveableStateProvider(LimitsDestination.PickAppForLimit.toString()) {
            AppPickerScreen(
                modifier = modifier,
                installedAppSource = installedAppSource,
                ownPackageName = ownPackageName,
                todayUsageSecondsByPackage = todayUsageSecondsByPackage,
                mode = AppPickerMode.Single(onPicked = { app ->
                    limitPackage = app.packageName
                    limitLabel = app.label
                    backStack.removeAt(backStack.lastIndex)
                }),
                onBack = { backStack.removeAt(backStack.lastIndex) }
            )
        }
        LimitsDestination.PickAppForSchedule -> saveableStateHolder.SaveableStateProvider(LimitsDestination.PickAppForSchedule.toString()) {
            AppPickerScreen(
                modifier = modifier,
                installedAppSource = installedAppSource,
                ownPackageName = ownPackageName,
                todayUsageSecondsByPackage = todayUsageSecondsByPackage,
                mode = AppPickerMode.Single(onPicked = { app ->
                    schedulePackage = app.packageName
                    scheduleLabel = app.label
                    backStack.removeAt(backStack.lastIndex)
                }),
                onBack = { backStack.removeAt(backStack.lastIndex) }
            )
        }
    }
}
