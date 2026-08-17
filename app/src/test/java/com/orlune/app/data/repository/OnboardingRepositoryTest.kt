package com.orlune.app.data.repository

import com.orlune.app.core.domain.focus.FocusNotificationPolicy
import com.orlune.app.core.domain.onboarding.OnboardingGoal
import com.orlune.app.data.local.dao.OnboardingStateDao
import com.orlune.app.data.local.entity.OnboardingStateEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingRepositoryTest {

    /** Mirrors Room's real singleton-row upsert semantics: one row, always id=0. */
    private class FakeOnboardingStateDao : OnboardingStateDao {
        val state = MutableStateFlow<OnboardingStateEntity?>(null)

        override suspend fun upsert(state: OnboardingStateEntity) {
            this.state.value = state
        }

        override fun observe(id: Int): Flow<OnboardingStateEntity?> = state
    }

    // --- first-launch state / completion persistence ---

    @Test
    fun `observeCompleted is false before any row has ever been written — true first launch`() = runTest {
        val repository = OnboardingRepository(FakeOnboardingStateDao())
        assertFalse(repository.observeCompleted().first())
    }

    @Test
    fun `complete marks onboarding as completed`() = runTest {
        val dao = FakeOnboardingStateDao()
        val repository = OnboardingRepository(dao)

        repository.complete(goals = emptySet(), customGoalText = "", focusNotificationPreference = FocusNotificationPolicy.ALLOW_ALL)

        assertTrue(dao.state.value?.completed == true)
    }

    // --- restart persistence: a fresh repository instance backed by the same
    // underlying row sees the same completed state, simulating a process restart
    // reading the same on-disk table. ---

    @Test
    fun `completion persists across a new repository instance over the same underlying row`() = runTest {
        val dao = FakeOnboardingStateDao()
        val firstInstance = OnboardingRepository(dao)
        firstInstance.complete(goals = setOf(OnboardingGoal.FOCUS), customGoalText = "", focusNotificationPreference = FocusNotificationPolicy.SILENCE_ALL)

        val restartedInstance = OnboardingRepository(dao)
        assertTrue(restartedInstance.observeCompleted().first())
        assertEquals(setOf(OnboardingGoal.FOCUS), restartedInstance.observeGoals().first())
    }

    // --- goal persistence ---

    @Test
    fun `complete persists multiple selected goals`() = runTest {
        val dao = FakeOnboardingStateDao()
        val repository = OnboardingRepository(dao)

        repository.complete(
            goals = setOf(OnboardingGoal.FOCUS, OnboardingGoal.STUDY, OnboardingGoal.REST),
            customGoalText = "",
            focusNotificationPreference = FocusNotificationPolicy.ALLOW_ALL
        )

        assertEquals(setOf(OnboardingGoal.FOCUS, OnboardingGoal.STUDY, OnboardingGoal.REST), repository.observeGoals().first())
    }

    @Test
    fun `complete with no goals selected persists an empty goal set, not a crash`() = runTest {
        val dao = FakeOnboardingStateDao()
        val repository = OnboardingRepository(dao)

        repository.complete(goals = emptySet(), customGoalText = "", focusNotificationPreference = FocusNotificationPolicy.ALLOW_ALL)

        assertTrue(repository.observeGoals().first().isEmpty())
    }

    @Test
    fun `complete trims custom goal text`() = runTest {
        val dao = FakeOnboardingStateDao()
        val repository = OnboardingRepository(dao)

        repository.complete(
            goals = setOf(OnboardingGoal.CUSTOM),
            customGoalText = "  Learn guitar  ",
            focusNotificationPreference = FocusNotificationPolicy.ALLOW_ALL
        )

        assertEquals("Learn guitar", dao.state.value?.customGoalText)
    }

    // --- focus notification choice persistence ---

    @Test
    fun `complete persists the chosen focus notification preference`() = runTest {
        val dao = FakeOnboardingStateDao()
        val repository = OnboardingRepository(dao)

        repository.complete(goals = emptySet(), customGoalText = "", focusNotificationPreference = FocusNotificationPolicy.ALLOW_CALLS)

        assertEquals(FocusNotificationPolicy.ALLOW_CALLS, repository.observeFocusNotificationPreference().first())
    }

    @Test
    fun `observeFocusNotificationPreference defaults to ALLOW_ALL before any row exists`() = runTest {
        val repository = OnboardingRepository(FakeOnboardingStateDao())
        assertEquals(FocusNotificationPolicy.ALLOW_ALL, repository.observeFocusNotificationPreference().first())
    }
}
