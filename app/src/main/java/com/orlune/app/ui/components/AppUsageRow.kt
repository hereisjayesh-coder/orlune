package com.orlune.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.orlune.app.platform.usage.AppDisplayInfo

/** An app's real icon, real label, and a usage duration — the icon-and-label-aware
 * replacement for the old label-only UsageLine row. Never shows a raw package name;
 * [display] is expected to already be resolved via AppDisplayResolver. */
@Composable
fun AppUsageRow(display: AppDisplayInfo, seconds: Long) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            AppIcon(display.icon)
            Spacer(modifier = Modifier.width(12.dp))
            Text(display.label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        }
        Text(formatDuration(seconds), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
