package com.orlune.app.feature.onboarding

/**
 * The 11 onboarding screens plus two side-trips (Privacy's "Privacy & Legal" button,
 * the app picker for Screen 8) — one shared back-stack, same
 * `mutableStateListOf<Destination>` pattern as FocusSection/LimitsSection/
 * SettingsSection, rather than a separate "linear step index" concept. The main flow
 * only ever pushes forward; the two side-trips push-and-pop back onto whichever
 * linear screen launched them.
 */
sealed class OnboardingDestination {
    data object Welcome : OnboardingDestination()
    data object WhatOrluneDoes : OnboardingDestination()
    data object Privacy : OnboardingDestination()
    data object UsageAccess : OnboardingDestination()
    data object Overlay : OnboardingDestination()
    data object FocusNotification : OnboardingDestination()
    data object Goal : OnboardingDestination()
    data object PickApps : OnboardingDestination()
    data object DailyLimit : OnboardingDestination()
    data object Finish : OnboardingDestination()
    data object LegalCenter : OnboardingDestination()
    data class LegalDocument(val id: String) : OnboardingDestination()
}
