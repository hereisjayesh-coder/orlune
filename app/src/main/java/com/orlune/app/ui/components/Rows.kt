package com.orlune.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.orlune.app.core.domain.focus.FocusSessionEngine
import com.orlune.app.data.local.entity.FocusSessionEntity
import com.orlune.app.platform.usage.AppDisplayInfo

/** [blockedAppDisplay] must already be resolved via [rememberAppDisplayInfos] — never
 * shows a raw package name, matching [AppUsageRow]'s contract. */
@Composable
fun SessionLine(session: FocusSessionEntity, blockedAppDisplay: Map<String, AppDisplayInfo>) {
    val state = FocusSessionEngine.stateOf(session, System.currentTimeMillis())
    val blockedPackages = FocusSessionEngine.blockedPackages(session)
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(state.name.lowercase().replaceFirstChar { it.uppercase() }, fontWeight = FontWeight.Medium)
        Row(verticalAlignment = Alignment.CenterVertically) {
            blockedPackages.forEach { packageName ->
                AppIcon(blockedAppDisplay[packageName]?.icon)
                Spacer(modifier = Modifier.width(4.dp))
            }
        }
        Text(
            "${formatDuration(session.completedMinutes * 60L)} of ${formatDuration(session.plannedMinutes * 60L)} · " +
                blockedPackages.joinToString { blockedAppDisplay[it]?.label ?: it },
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun PermissionLine(label: String, granted: Boolean, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) { Text(label); Text(if (granted) "Granted" else "Not granted", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        if (!granted) TextButton(onClick = onClick) { Text("Open") }
    }
}

@Composable
fun ComparisonLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(label); Text(value, fontWeight = FontWeight.Medium) }
}
