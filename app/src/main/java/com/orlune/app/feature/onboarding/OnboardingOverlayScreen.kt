package com.orlune.app.feature.onboarding

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun OnboardingOverlayScreen(
    modifier: Modifier,
    granted: Boolean,
    onEnable: () -> Unit,
    onContinue: () -> Unit
) {
    OnboardingScaffold(
        modifier = modifier,
        title = "Blocking screen",
        primaryLabel = if (granted) "Continue" else "Enable",
        onPrimary = if (granted) onContinue else onEnable,
        secondaryLabel = if (granted) null else "Skip for now",
        onSecondary = if (granted) null else onContinue
    ) {
        Column {
            Text(
                "To interrupt a distracting app when a limit is reached, Orlune may need permission to display its blocking screen.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            OnboardingBullet("Shows a simple screen over a distracting app once a limit is reached")
            OnboardingBullet("Only appears when a rule you set is actually triggered")
            OnboardingBullet("Orlune does not record your screen")
            if (granted) {
                Spacer(modifier = Modifier.height(4.dp))
                Text("Granted", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
            } else {
                Text(
                    "You can skip this — Orlune stays usable, but it won't be able to show its blocking screen until you enable this later from Settings.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
