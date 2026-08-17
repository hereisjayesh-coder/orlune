package com.orlune.app.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.orlune.app.core.domain.focus.FocusNotificationPolicy

/** Display label matching the exact wording from the Focus screen's UX spec. */
fun FocusNotificationPolicy.label(): String = when (this) {
    FocusNotificationPolicy.ALLOW_ALL -> "Allow all"
    FocusNotificationPolicy.SILENCE_ALL -> "Silence all"
    FocusNotificationPolicy.ALLOW_CALLS -> "Allow calls"
    FocusNotificationPolicy.ALLOW_CALLS_AND_SELECTED -> "Allow calls + selected apps"
}

/** Display order matches the UX spec's numbered list (1-4), independent of
 * [FocusNotificationPolicy.restrictiveness]'s ordering, which exists for a different
 * purpose (resolving overlapping sessions), not for how the chips are laid out. */
private val DISPLAY_ORDER = listOf(
    FocusNotificationPolicy.ALLOW_ALL,
    FocusNotificationPolicy.SILENCE_ALL,
    FocusNotificationPolicy.ALLOW_CALLS,
    FocusNotificationPolicy.ALLOW_CALLS_AND_SELECTED
)

@Composable
fun NotificationPolicySelector(selected: FocusNotificationPolicy, onSelect: (FocusNotificationPolicy) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
        DISPLAY_ORDER.forEach { policy ->
            FilterChip(selected = policy == selected, onClick = { onSelect(policy) }, label = { Text(policy.label()) })
        }
    }
}
