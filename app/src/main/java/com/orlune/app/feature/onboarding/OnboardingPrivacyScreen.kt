package com.orlune.app.feature.onboarding

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun OnboardingPrivacyScreen(modifier: Modifier, onContinue: () -> Unit, onOpenPrivacyAndLegal: () -> Unit) {
    OnboardingScaffold(
        modifier = modifier,
        title = "Your data stays on this device",
        primaryLabel = "Continue",
        onPrimary = onContinue,
        secondaryLabel = "Privacy & Legal",
        onSecondary = onOpenPrivacyAndLegal
    ) {
        Column {
            OnboardingBullet("No account, no login, no signup")
            OnboardingBullet("No cloud — nothing leaves this device")
            OnboardingBullet("No advertising")
            OnboardingBullet("No AI")
            OnboardingBullet("No usage data is ever uploaded anywhere")
            OnboardingBullet("Everything is stored in a local database on this device")
            OnboardingBullet("You can export or delete your data at any time, from Settings")
        }
    }
}
