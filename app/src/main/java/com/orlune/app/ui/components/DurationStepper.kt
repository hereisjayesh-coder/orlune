package com.orlune.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/** Pure stepper math, extracted so it's unit-testable without Compose: applying
 * [delta] to [current] always lands within [range], never drifting outside it
 * regardless of how many times a +/- button is tapped. */
fun steppedValue(current: Int, delta: Int, range: IntRange): Int = (current + delta).coerceIn(range)

/**
 * A tap-based +/- stepper for a bounded integer value — replaces free-text numeric
 * entry (e.g. for a custom duration's hours/minutes) with a control that can't
 * produce invalid text, out-of-range values, or a confusing empty field.
 */
@Composable
fun IntStepper(
    label: String,
    value: Int,
    range: IntRange,
    step: Int = 1,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onValueChange(steppedValue(value, -step, range)) }, enabled = value > range.first) {
                Icon(Icons.Filled.Remove, contentDescription = "Decrease $label")
            }
            Text(
                value.toString().padStart(2, '0'),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(48.dp)
            )
            IconButton(onClick = { onValueChange(steppedValue(value, step, range)) }, enabled = value < range.last) {
                Icon(Icons.Filled.Add, contentDescription = "Increase $label")
            }
        }
    }
}

/** A custom-duration selector combining an hours and a minutes IntStepper — produces
 * values like "2h", "3h 30m", "4h" (via [formatDuration] on the resulting seconds)
 * without any free-text entry. Minutes steps in 5-minute increments, matching how
 * daily-limit durations are actually chosen in practice. */
@Composable
fun DurationStepper(
    hours: Int,
    minutes: Int,
    onHoursChange: (Int) -> Unit,
    onMinutesChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        IntStepper("Hours", hours, range = 0..24, onValueChange = onHoursChange, modifier = Modifier.weight(1f))
        IntStepper("Minutes", minutes, range = 0..55, step = 5, onValueChange = onMinutesChange, modifier = Modifier.weight(1f))
    }
}
