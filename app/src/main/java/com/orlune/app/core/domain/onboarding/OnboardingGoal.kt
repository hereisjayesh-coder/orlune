package com.orlune.app.core.domain.onboarding

/**
 * A user-picked "what would you like more time for" tag from onboarding Screen 7.
 * Purely descriptive — no engine reads this today. Persisted so a later phase can use
 * it to personalize *optional* suggestions (per the onboarding spec: "used to
 * personalize optional intervention suggestions later"); nothing in this codebase
 * currently branches on it, and it must never gate or alter enforcement behavior.
 */
enum class OnboardingGoal {
    FOCUS, STUDY, LEARN, COMMUNICATION, RESET, MOVE, CREATE, READ, REST, CUSTOM;

    companion object {
        /** Unrecognized values are dropped rather than crashing — same fail-safe
         * shape as [com.orlune.app.core.domain.focus.FocusNotificationPolicy.fromStored]. */
        fun fromStored(value: String): OnboardingGoal? = entries.find { it.name == value }
    }
}

/** Parses a comma-separated [OnboardingGoal] name list — same split/trim/filter shape
 * used throughout this codebase for comma-separated persisted fields (e.g.
 * `FocusSessionEngine.blockedPackages`). Unrecognized tokens are silently dropped. */
fun parseOnboardingGoals(stored: String): Set<OnboardingGoal> =
    stored.split(",")
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .mapNotNull { OnboardingGoal.fromStored(it) }
        .toSet()
