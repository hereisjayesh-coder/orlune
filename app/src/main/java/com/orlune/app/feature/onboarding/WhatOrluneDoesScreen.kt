package com.orlune.app.feature.onboarding

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun WhatOrluneDoesScreen(modifier: Modifier, onContinue: () -> Unit) {
    OnboardingScaffold(
        modifier = modifier,
        title = "What Orlune does",
        primaryLabel = "Continue",
        onPrimary = onContinue
    ) {
        Column {
            OnboardingBullet("Tracks app usage locally")
            OnboardingBullet("Helps you set limits")
            OnboardingBullet("Can interrupt distracting apps")
            OnboardingBullet("Provides Focus sessions")
            OnboardingBullet("Keeps usage data on this device")
        }
    }
}
