package com.orlune.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Single-row onboarding record (always `id = SINGLETON_ID`, same pattern as
 * [ThemePreferenceEntity]). Absence of a row (not just `completed = false`) is what
 * "first launch" actually means — [com.orlune.app.data.repository.OnboardingRepository]
 * treats a missing row identically to `completed = false`, so there's only one
 * "not completed yet" state to reason about, not two.
 *
 * [goals]/[customGoalText]/[focusNotificationPreference] are written once, at the
 * "Finish" screen, not incrementally per onboarding step — an interrupted onboarding
 * (process death, force-quit) simply restarts from Welcome next launch with nothing
 * partially committed, rather than resuming into an inconsistent partial state.
 */
@Entity(tableName = "onboarding_state")
data class OnboardingStateEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val completed: Boolean,
    /** Comma-separated [com.orlune.app.core.domain.onboarding.OnboardingGoal] names. */
    val goals: String,
    /** Only meaningful when [goals] contains `CUSTOM`. */
    val customGoalText: String,
    /** A [com.orlune.app.core.domain.focus.FocusNotificationPolicy] name — the
     * conceptual choice previewed at onboarding Screen 6, reused as Focus's initial
     * suggested policy the first time the user opens the real Focus screen (still
     * freely changeable per session there; this is a starting point, not a lock-in). */
    val focusNotificationPreference: String
) {
    companion object {
        const val SINGLETON_ID = 0
    }
}
