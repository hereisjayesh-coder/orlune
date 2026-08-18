package com.orlune.app.feature.onboarding

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.orlune.app.core.domain.onboarding.OnboardingDailyLimit
import com.orlune.app.ui.components.DurationStepper
import com.orlune.app.ui.components.formatDuration

private val presetMinutes = listOf(30, 45, 60, 90)

/**
 * Same preset/custom shape as `LimitsScreen`'s daily-limit form (30/45/60/90 chips +
 * `DurationStepper` for Custom) — this is a starting point, changeable anytime from
 * the real Limits screen, never a lock-in.
 */
@Composable
fun OnboardingDailyLimitScreen(
    modifier: Modifier,
    selectedPresetMinutes: Int?,
    customSelected: Boolean,
    customHours: Int,
    customMinutes: Int,
    onSelectPreset: (Int) -> Unit,
    onSelectCustom: () -> Unit,
    onCustomHoursChange: (Int) -> Unit,
    onCustomMinutesChange: (Int) -> Unit,
    onContinue: () -> Unit,
    onSkip: () -> Unit
) {
    val currentHours = if (customSelected) customHours else (selectedPresetMinutes ?: 0) / 60
    val currentMinutes = if (customSelected) customMinutes else (selectedPresetMinutes ?: 0) % 60
    val previewSeconds = (currentHours * 3600L) + (currentMinutes * 60L)
    // Presets (30/45/60/90) are always valid; only a hand-set Custom duration can be
    // 0 — disabling Continue here (rather than silently discarding it at Finish, the
    // previous behavior) is what actually satisfies "reject 0 minutes."
    val validDuration = OnboardingDailyLimit.isValidDuration(currentHours, currentMinutes)

    OnboardingScaffold(
        modifier = modifier,
        title = "Daily limit",
        subtitle = "This is your starting limit. You can change it anytime.",
        primaryLabel = "Continue",
        onPrimary = onContinue,
        primaryEnabled = validDuration,
        secondaryLabel = "Skip for now",
        onSecondary = onSkip
    ) {
        Column {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                presetMinutes.forEach { preset ->
                    FilterChip(
                        selected = !customSelected && selectedPresetMinutes == preset,
                        onClick = { onSelectPreset(preset) },
                        label = { Text("${preset}m") }
                    )
                }
                FilterChip(selected = customSelected, onClick = onSelectCustom, label = { Text("Custom") })
            }
            if (customSelected) {
                Spacer(modifier = Modifier.height(16.dp))
                DurationStepper(
                    hours = customHours,
                    minutes = customMinutes,
                    onHoursChange = onCustomHoursChange,
                    onMinutesChange = onCustomMinutesChange
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            if (validDuration) {
                Text(
                    "= ${formatDuration(previewSeconds)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    "Set a duration of at least 1 minute to continue.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
