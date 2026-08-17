package com.orlune.app.feature.onboarding

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.orlune.app.core.domain.focus.FocusNotificationPolicy
import com.orlune.app.ui.components.InfoCard
import com.orlune.app.ui.components.NotificationPolicySelector
import com.orlune.app.ui.components.label

/**
 * Purely a preview/introduction to the same choices the real Focus screen offers
 * (Phase 8, `core/domain/focus/FocusNotificationPolicy.kt`/`NotificationPolicySelector`)
 * — nothing here enforces anything by itself. Selecting a policy other than "Allow
 * all" never requests the permission automatically; it only surfaces the same
 * disclosure + Settings button the real Focus screen already shows, so the user can
 * pre-grant it now if they want, or skip entirely with zero permission requested.
 */
@Composable
fun OnboardingFocusNotificationScreen(
    modifier: Modifier,
    selected: FocusNotificationPolicy,
    onSelect: (FocusNotificationPolicy) -> Unit,
    notificationPolicyAccessGranted: Boolean,
    onOpenNotificationPolicySettings: () -> Unit,
    onContinue: () -> Unit
) {
    OnboardingScaffold(
        modifier = modifier,
        title = "Focus notifications",
        primaryLabel = "Continue",
        onPrimary = onContinue
    ) {
        Column {
            Text(
                "Focus can silence interruptions while you work.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Orlune does not read or store notification content.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            NotificationPolicySelector(selected = selected, onSelect = onSelect)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "This is optional and entirely separate from the rest of Orlune — you're not required to set this up now, and it can be changed every time you start a Focus session.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (selected != FocusNotificationPolicy.ALLOW_ALL && !notificationPolicyAccessGranted) {
                Spacer(modifier = Modifier.height(12.dp))
                InfoCard(
                    "Notification access needed",
                    "Focus can silence interruptions while you work. Orlune does not read or store your notification content.",
                    "Open notification settings",
                    onOpenNotificationPolicySettings
                )
            } else if (selected != FocusNotificationPolicy.ALLOW_ALL) {
                Spacer(modifier = Modifier.height(12.dp))
                Text("\"${selected.label()}\" is ready — access already granted.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
