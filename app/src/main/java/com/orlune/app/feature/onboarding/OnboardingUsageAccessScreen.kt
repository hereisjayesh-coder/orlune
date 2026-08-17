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
fun OnboardingUsageAccessScreen(
    modifier: Modifier,
    granted: Boolean,
    onGrant: () -> Unit,
    onContinue: () -> Unit
) {
    OnboardingScaffold(
        modifier = modifier,
        title = "Usage Access",
        primaryLabel = if (granted) "Continue" else "Grant Usage Access",
        onPrimary = if (granted) onContinue else onGrant,
        secondaryLabel = if (granted) null else "Not now",
        onSecondary = if (granted) null else onContinue
    ) {
        Column {
            Text(
                "Orlune needs Usage Access to measure how long apps are used. It does not read your screen or capture screenshots.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (granted) {
                Spacer(modifier = Modifier.height(12.dp))
                Text("Granted", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
            } else {
                Text(
                    "You can skip this for now — usage-based features (Home, Insights, Limits) will stay unavailable until you grant it, but the rest of Orlune still works. You can grant it anytime from Settings.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
