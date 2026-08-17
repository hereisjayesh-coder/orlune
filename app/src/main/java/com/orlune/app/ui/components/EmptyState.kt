package com.orlune.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun EmptyState(title: String, body: String) {
    Column(modifier = Modifier.padding(vertical = 12.dp)) { Text(title, fontWeight = FontWeight.Medium); Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant) }
}
