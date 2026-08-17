package com.orlune.app.feature.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun OnboardingFinishScreen(
    modifier: Modifier,
    goalSummary: String,
    appCount: Int,
    dailyLimitSummary: String,
    focusNotificationSummary: String,
    onStartUsingOrlune: () -> Unit
) {
    OnboardingScaffold(
        modifier = modifier,
        title = "Your Orlune setup",
        primaryLabel = "Start using Orlune",
        onPrimary = onStartUsingOrlune
    ) {
        Column {
            SummaryRow("Goal", goalSummary)
            SummaryRow("Apps", if (appCount == 0) "None selected yet" else "$appCount")
            SummaryRow("Daily limit", dailyLimitSummary)
            SummaryRow("Focus notifications", focusNotificationSummary)
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        Text(value, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
