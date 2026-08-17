package com.orlune.app.data.repository

import com.orlune.app.core.domain.focus.FocusNotificationPolicy
import com.orlune.app.core.domain.onboarding.OnboardingGoal
import com.orlune.app.core.domain.onboarding.parseOnboardingGoals
import com.orlune.app.data.local.dao.OnboardingStateDao
import com.orlune.app.data.local.entity.OnboardingStateEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Owns the durable "has this device completed onboarding" flag plus the small set of
 * choices captured at the Finish screen. A missing row (never written yet — true
 * first launch) and an explicit `completed = false` row are deliberately
 * indistinguishable here: [observeCompleted] treats both as "show onboarding," so
 * callers never need to special-case "row doesn't exist yet."
 */
class OnboardingRepository(private val onboardingStateDao: OnboardingStateDao) {

    fun observeCompleted(): Flow<Boolean> = onboardingStateDao.observe().map { it?.completed == true }

    fun observeGoals(): Flow<Set<OnboardingGoal>> =
        onboardingStateDao.observe().map { parseOnboardingGoals(it?.goals.orEmpty()) }

    fun observeFocusNotificationPreference(): Flow<FocusNotificationPolicy> =
        onboardingStateDao.observe().map { FocusNotificationPolicy.fromStored(it?.focusNotificationPreference.orEmpty()) }

    /** Commits every onboarding selection at once (Finish, screen 10) and marks
     * onboarding complete in the same write — see [OnboardingStateEntity]'s KDoc for
     * why this is all-or-nothing rather than incremental per screen. */
    suspend fun complete(
        goals: Set<OnboardingGoal>,
        customGoalText: String,
        focusNotificationPreference: FocusNotificationPolicy
    ) {
        onboardingStateDao.upsert(
            OnboardingStateEntity(
                completed = true,
                goals = goals.joinToString(",") { it.name },
                customGoalText = customGoalText.trim(),
                focusNotificationPreference = focusNotificationPreference.name
            )
        )
    }
}
