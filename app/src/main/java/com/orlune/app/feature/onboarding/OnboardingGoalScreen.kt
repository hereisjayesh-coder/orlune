package com.orlune.app.feature.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.orlune.app.core.domain.onboarding.OnboardingGoal

fun OnboardingGoal.emoji(): String = when (this) {
    OnboardingGoal.FOCUS -> "🎯"
    OnboardingGoal.STUDY -> "📚"
    OnboardingGoal.LEARN -> "🧠"
    OnboardingGoal.COMMUNICATION -> "🗣"
    OnboardingGoal.RESET -> "🧘"
    OnboardingGoal.MOVE -> "🚶"
    OnboardingGoal.CREATE -> "✍️"
    OnboardingGoal.READ -> "📖"
    OnboardingGoal.REST -> "🌙"
    OnboardingGoal.CUSTOM -> ""
}

fun OnboardingGoal.label(): String = when (this) {
    OnboardingGoal.FOCUS -> "Focus"
    OnboardingGoal.STUDY -> "Study"
    OnboardingGoal.LEARN -> "Learn"
    OnboardingGoal.COMMUNICATION -> "Communication"
    OnboardingGoal.RESET -> "Reset"
    OnboardingGoal.MOVE -> "Move"
    OnboardingGoal.CREATE -> "Create"
    OnboardingGoal.READ -> "Read"
    OnboardingGoal.REST -> "Rest"
    OnboardingGoal.CUSTOM -> "Custom"
}

@Composable
fun OnboardingGoalScreen(
    modifier: Modifier,
    selected: Set<OnboardingGoal>,
    onToggle: (OnboardingGoal) -> Unit,
    customText: String,
    onCustomTextChange: (String) -> Unit,
    onContinue: () -> Unit
) {
    OnboardingScaffold(
        modifier = modifier,
        title = "What would you like more time for?",
        subtitle = "Optional — pick as many as you like, or skip.",
        primaryLabel = "Continue",
        onPrimary = onContinue
    ) {
        Column {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.wrapContentSize()) {
                OnboardingGoal.entries.forEach { goal ->
                    FilterChip(
                        selected = goal in selected,
                        onClick = { onToggle(goal) },
                        label = { Text(if (goal == OnboardingGoal.CUSTOM) goal.label() else "${goal.emoji()} ${goal.label()}") }
                    )
                }
            }
            if (OnboardingGoal.CUSTOM in selected) {
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = customText,
                    onValueChange = onCustomTextChange,
                    label = { Text("Your own goal") },
                    modifier = Modifier
                )
            }
        }
    }
}
