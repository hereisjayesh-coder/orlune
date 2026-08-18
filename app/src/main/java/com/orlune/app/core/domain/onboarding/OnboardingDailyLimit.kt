package com.orlune.app.core.domain.onboarding

import com.orlune.app.core.domain.rules.DailyLimitInput

/**
 * What onboarding's Finish screen actually persists for the daily-limit step,
 * extracted as pure logic (same reasoning as [com.orlune.app.ui.components.steppedValue])
 * so "skip vs preset vs invalid custom duration vs no apps selected" is unit-testable
 * without Compose. Previously this branching lived inline in `OnboardingSection`, used
 * `Result.getOrNull()` to silently swallow an invalid (e.g. 0-minute) custom duration,
 * and had no test coverage — a 0-minute custom entry could reach "Continue" and would
 * then silently vanish at Finish with no rule created and no error shown.
 */
object OnboardingDailyLimit {
    data class Plan(val packages: List<String>, val thresholdSeconds: Long)

    fun isValidDuration(hours: Int, minutes: Int): Boolean =
        DailyLimitInput.toThresholdSeconds(hours, minutes).isSuccess

    /** Null whenever there's nothing to persist: skipped, no apps selected, or an
     * invalid duration slipped through some other path (defense in depth — the UI
     * should already prevent reaching here with an invalid duration via [isValidDuration]). */
    fun plan(skipped: Boolean, hours: Int, minutes: Int, selectedPackages: List<String>): Plan? {
        if (skipped || selectedPackages.isEmpty()) return null
        val seconds = DailyLimitInput.toThresholdSeconds(hours, minutes).getOrNull() ?: return null
        return Plan(selectedPackages, seconds)
    }
}
