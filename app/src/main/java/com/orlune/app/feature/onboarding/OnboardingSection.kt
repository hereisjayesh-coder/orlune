package com.orlune.app.feature.onboarding

import android.content.ActivityNotFoundException
import androidx.activity.compose.BackHandler
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.orlune.app.core.domain.onboarding.OnboardingDailyLimit
import com.orlune.app.core.domain.onboarding.OnboardingGoal
import com.orlune.app.core.domain.onboarding.parseOnboardingGoals
import com.orlune.app.feature.apppicker.AppPickerMode
import com.orlune.app.feature.apppicker.AppPickerScreen
import com.orlune.app.feature.privacy.LegalCenterScreen
import com.orlune.app.feature.privacy.LegalDocumentScreen
import com.orlune.app.platform.usage.InstalledAppSource
import com.orlune.app.ui.components.formatDuration
import com.orlune.app.ui.components.label

/**
 * Owns the entire first-launch onboarding flow: an 11-screen linear sequence plus two
 * side-trips (the app picker, the Privacy & Legal Center), one shared back-stack —
 * same `mutableStateListOf<Destination>` + `rememberSaveableStateHolder` pattern as
 * FocusSection/LimitsSection/SettingsSection. Every selection lives in plain,
 * Bundle-safe `rememberSaveable` state (Strings/Ints/Booleans only — enums stored as
 * their `.name`, matching FocusSection's established precedent after this project's
 * prior `SaveableStateProvider` crash) and is committed to Room *once*, atomically, at
 * "Finish" — an onboarding session interrupted by process death simply restarts from
 * Welcome next launch with nothing partially written, never a stuck half-state.
 */
@Composable
fun OnboardingSection(
    modifier: Modifier,
    installedAppSource: InstalledAppSource,
    ownPackageName: String,
    todayUsageSecondsByPackage: Map<String, Long>,
    usageAccessGranted: Boolean,
    overlayGranted: Boolean,
    notificationPolicyAccessGranted: Boolean,
    onOpenUsageAccessSettings: () -> Unit,
    onOpenOverlaySettings: () -> Unit,
    onOpenNotificationPolicySettings: () -> Unit,
    onComplete: (
        goals: Set<OnboardingGoal>,
        customGoalText: String,
        focusNotificationPreference: FocusNotificationPolicy,
        dailyLimitPlan: OnboardingDailyLimit.Plan?
    ) -> Unit
) {
    val backStack = remember { mutableStateListOf<OnboardingDestination>(OnboardingDestination.Welcome) }
    BackHandler(enabled = backStack.size > 1) { backStack.removeAt(backStack.lastIndex) }
    val saveableStateHolder = rememberSaveableStateHolder()

    // Goals: stored as a comma-joined String (Bundle-safe), same shape as
    // ScheduleEntity.daysOfWeek/FocusSessionEntity.blockedPackages elsewhere in this
    // codebase — never a raw Set<Enum> in saveable state.
    var goalsText by rememberSaveable { mutableStateOf("") }
    val selectedGoals = remember(goalsText) { parseOnboardingGoals(goalsText) }
    var customGoalText by rememberSaveable { mutableStateOf("") }

    // Package names only, comma-joined (Bundle-safe) — same shape as goalsText above.
    // Previously this held the full List<InstalledApp> in plain `remember`, which does
    // NOT survive process death: a killed-and-restored onboarding session would lose
    // the app selection while every other rememberSaveable field on this screen
    // (goals, custom duration, notification policy) survived intact. Only the package
    // name is ever actually needed downstream (dailyLimitPlan, appCount).
    var selectedAppPackagesText by rememberSaveable { mutableStateOf("") }
    val selectedPackages = remember(selectedAppPackagesText) {
        selectedAppPackagesText.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }

    var limitSkipped by rememberSaveable { mutableStateOf(false) }
    var selectedPresetMinutes by rememberSaveable { mutableStateOf<Int?>(60) }
    var customLimitSelected by rememberSaveable { mutableStateOf(false) }
    var customLimitHours by rememberSaveable { mutableStateOf(0) }
    var customLimitMinutes by rememberSaveable { mutableStateOf(0) }

    var focusNotificationPolicyName by rememberSaveable { mutableStateOf(FocusNotificationPolicy.ALLOW_ALL.name) }
    val focusNotificationPolicy = FocusNotificationPolicy.fromStored(focusNotificationPolicyName)

    var settingsUnavailable by remember { mutableStateOf(false) }
    val openSettingsSafely: (() -> Unit) -> Unit = { open ->
        try {
            open()
        } catch (_: ActivityNotFoundException) {
            settingsUnavailable = true
        }
    }

    val dailyLimitHours = if (customLimitSelected) customLimitHours else (selectedPresetMinutes ?: 0) / 60
    val dailyLimitMinutes = if (customLimitSelected) customLimitMinutes else (selectedPresetMinutes ?: 0) % 60
    val dailyLimitPlan = remember(limitSkipped, dailyLimitHours, dailyLimitMinutes, selectedPackages) {
        OnboardingDailyLimit.plan(limitSkipped, dailyLimitHours, dailyLimitMinutes, selectedPackages)
    }

    when (val destination = backStack.last()) {
        OnboardingDestination.Welcome -> saveableStateHolder.SaveableStateProvider(destination.toString()) {
            WelcomeScreen(modifier = modifier, onGetStarted = { backStack.add(OnboardingDestination.WhatOrluneDoes) })
        }
        OnboardingDestination.WhatOrluneDoes -> saveableStateHolder.SaveableStateProvider(destination.toString()) {
            WhatOrluneDoesScreen(modifier = modifier, onContinue = { backStack.add(OnboardingDestination.Privacy) })
        }
        OnboardingDestination.Privacy -> saveableStateHolder.SaveableStateProvider(destination.toString()) {
            OnboardingPrivacyScreen(
                modifier = modifier,
                onContinue = { backStack.add(OnboardingDestination.UsageAccess) },
                onOpenPrivacyAndLegal = { backStack.add(OnboardingDestination.LegalCenter) }
            )
        }
        OnboardingDestination.UsageAccess -> saveableStateHolder.SaveableStateProvider(destination.toString()) {
            OnboardingUsageAccessScreen(
                modifier = modifier,
                granted = usageAccessGranted,
                onGrant = { openSettingsSafely(onOpenUsageAccessSettings) },
                onContinue = { backStack.add(OnboardingDestination.Overlay) }
            )
        }
        OnboardingDestination.Overlay -> saveableStateHolder.SaveableStateProvider(destination.toString()) {
            OnboardingOverlayScreen(
                modifier = modifier,
                granted = overlayGranted,
                onEnable = { openSettingsSafely(onOpenOverlaySettings) },
                onContinue = { backStack.add(OnboardingDestination.FocusNotification) }
            )
        }
        OnboardingDestination.FocusNotification -> saveableStateHolder.SaveableStateProvider(destination.toString()) {
            OnboardingFocusNotificationScreen(
                modifier = modifier,
                selected = focusNotificationPolicy,
                onSelect = { focusNotificationPolicyName = it.name },
                notificationPolicyAccessGranted = notificationPolicyAccessGranted,
                onOpenNotificationPolicySettings = { openSettingsSafely(onOpenNotificationPolicySettings) },
                onContinue = { backStack.add(OnboardingDestination.Goal) }
            )
        }
        OnboardingDestination.Goal -> saveableStateHolder.SaveableStateProvider(destination.toString()) {
            OnboardingGoalScreen(
                modifier = modifier,
                selected = selectedGoals,
                onToggle = { goal ->
                    val next = if (goal in selectedGoals) selectedGoals - goal else selectedGoals + goal
                    goalsText = next.joinToString(",") { it.name }
                },
                customText = customGoalText,
                onCustomTextChange = { customGoalText = it },
                onContinue = { backStack.add(OnboardingDestination.PickApps) }
            )
        }
        OnboardingDestination.PickApps -> saveableStateHolder.SaveableStateProvider(destination.toString()) {
            AppPickerScreen(
                modifier = modifier,
                installedAppSource = installedAppSource,
                ownPackageName = ownPackageName,
                todayUsageSecondsByPackage = todayUsageSecondsByPackage,
                title = "Which apps steal your time?",
                subtitle = "For example: Instagram, YouTube, Facebook, Reddit.",
                mode = AppPickerMode.Multi(
                    initialSelection = selectedPackages.toSet(),
                    onConfirm = { picked ->
                        selectedAppPackagesText = picked.joinToString(",") { it.packageName }
                        backStack.add(OnboardingDestination.DailyLimit)
                    }
                ),
                onSkip = { backStack.add(OnboardingDestination.DailyLimit) },
                onBack = { backStack.removeAt(backStack.lastIndex) }
            )
        }
        OnboardingDestination.DailyLimit -> saveableStateHolder.SaveableStateProvider(destination.toString()) {
            OnboardingDailyLimitScreen(
                modifier = modifier,
                selectedPresetMinutes = selectedPresetMinutes,
                customSelected = customLimitSelected,
                customHours = customLimitHours,
                customMinutes = customLimitMinutes,
                onSelectPreset = { selectedPresetMinutes = it; customLimitSelected = false; limitSkipped = false },
                onSelectCustom = { customLimitSelected = true; limitSkipped = false },
                onCustomHoursChange = { customLimitHours = it; limitSkipped = false },
                onCustomMinutesChange = { customLimitMinutes = it; limitSkipped = false },
                onContinue = { limitSkipped = false; backStack.add(OnboardingDestination.Finish) },
                onSkip = { limitSkipped = true; backStack.add(OnboardingDestination.Finish) }
            )
        }
        OnboardingDestination.Finish -> saveableStateHolder.SaveableStateProvider(destination.toString()) {
            OnboardingFinishScreen(
                modifier = modifier,
                goalSummary = onboardingGoalSummary(selectedGoals, customGoalText),
                appCount = selectedPackages.size,
                dailyLimitSummary = if (dailyLimitPlan != null) formatDuration(dailyLimitPlan.thresholdSeconds) else "Not set",
                focusNotificationSummary = focusNotificationPolicy.label(),
                onStartUsingOrlune = {
                    onComplete(selectedGoals, customGoalText, focusNotificationPolicy, dailyLimitPlan)
                }
            )
        }
        OnboardingDestination.LegalCenter -> saveableStateHolder.SaveableStateProvider(destination.toString()) {
            LegalCenterScreen(
                modifier = modifier,
                onBack = { backStack.removeAt(backStack.lastIndex) },
                onOpenDocument = { id -> backStack.add(OnboardingDestination.LegalDocument(id)) }
            )
        }
        is OnboardingDestination.LegalDocument -> saveableStateHolder.SaveableStateProvider(destination.toString()) {
            LegalDocumentScreen(
                modifier = modifier,
                documentId = destination.id,
                onBack = { backStack.removeAt(backStack.lastIndex) }
            )
        }
    }

    if (settingsUnavailable) {
        AlertDialog(
            onDismissRequest = { settingsUnavailable = false },
            title = { Text("Setting unavailable") },
            text = { Text("This setting isn't available on this device. You can continue without it and try again later from Settings.") },
            confirmButton = { TextButton(onClick = { settingsUnavailable = false }) { Text("OK") } }
        )
    }
}

private fun onboardingGoalSummary(goals: Set<OnboardingGoal>, customText: String): String {
    if (goals.isEmpty()) return "Not set"
    val labels = goals.map { if (it == OnboardingGoal.CUSTOM && customText.isNotBlank()) customText.trim() else it.label() }
    return labels.joinToString(", ")
}
