package com.orlune.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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

@Composable
fun SessionLine(session: FocusSessionEntity) {
    val state = FocusSessionEngine.stateOf(session, System.currentTimeMillis())
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(state.name.lowercase().replaceFirstChar { it.uppercase() }, fontWeight = FontWeight.Medium)
        Text("${formatDuration(session.completedMinutes * 60L)} of ${formatDuration(session.plannedMinutes * 60L)} · ${FocusSessionEngine.blockedPackages(session).joinToString()}", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
