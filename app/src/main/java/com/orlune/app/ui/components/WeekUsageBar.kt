package com.orlune.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

/**
 * One restrained, single-color proportional bar — deliberately not a chart library:
 * a flat track plus a filled portion is legible at a glance and matches the
 * black/minimal visual language everywhere else (no gradients, no per-segment
 * colors). [seconds] relative to [maxSeconds] (typically the largest value across the
 * set of bars being compared) decides the filled fraction; a non-positive [maxSeconds]
 * renders an empty track rather than dividing by zero.
 */
@Composable
fun WeekUsageBar(seconds: Long, maxSeconds: Long, modifier: Modifier = Modifier) {
    val fraction = if (maxSeconds > 0) (seconds.toFloat() / maxSeconds.toFloat()).coerceIn(0f, 1f) else 0f
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.primary)
        )
    }
}
