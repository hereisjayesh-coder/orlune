package com.orlune.app.data.repository

import com.orlune.app.core.domain.focus.FocusNotificationPolicy
import com.orlune.app.core.domain.focus.FocusSessionState
import com.orlune.app.core.domain.focus.FocusSessionEngine
import com.orlune.app.core.domain.focus.allowedNotificationPackages
import com.orlune.app.data.local.dao.FocusSessionDao
import com.orlune.app.data.local.entity.FocusSessionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Test

class FocusSessionRepositoryTest {

    /** Mutable, keyed by id (mirroring Room's real upsert-by-primary-key semantics). */
    private class FakeFocusSessionDao : FocusSessionDao {
        val sessions = linkedMapOf<Long, FocusSessionEntity>()
        private var nextId = 1L

        override suspend fun upsert(session: FocusSessionEntity): Long {
            val id = if (session.id == 0L) nextId++ else session.id
            sessions[id] = session.copy(id = id)
            return id
        }

        override suspend fun delete(session: FocusSessionEntity) {
            sessions.remove(session.id)
        }

        override fun observeAll(): Flow<List<FocusSessionEntity>> = flowOf(sessions.values.toList())
    }

    @Test
    fun `startSession persists a session with zero completed minutes and no end`() = runTest {
        var now = 1_000_000L
        val dao = FakeFocusSessionDao()
        val repository = FocusSessionRepository(dao, nowMillis = { now })

        val id = repository.startSession(plannedMinutes = 25, blockedPackages = listOf("app.a", "app.b"))

        val stored = dao.sessions.getValue(id)
        assertEquals(0, stored.completedMinutes)
        assertNull(stored.endTs)
        assertEquals(setOf("app.a", "app.b"), FocusSessionEngine.blockedPackages(stored))
        assertEquals(now, stored.startTs)
    }

    @Test
    fun `reconcileActiveSessions advances completedMinutes for an active session`() = runTest {
        var now = 0L
        val dao = FakeFocusSessionDao()
        val repository = FocusSessionRepository(dao, nowMillis = { now })
        val id = repository.startSession(plannedMinutes = 25, blockedPackages = listOf("app.a"), startAt = 0)

        now = 10 * 60_000L
        repository.reconcileActiveSessions()

        val stored = dao.sessions.getValue(id)
        assertEquals(10, stored.completedMinutes)
        assertNull(stored.endTs)
        assertEquals(FocusSessionState.ACTIVE, FocusSessionEngine.stateOf(stored, now))
    }

    @Test
    fun `reconcileActiveSessions finalizes a session that reached its planned duration`() = runTest {
        var now = 0L
        val dao = FakeFocusSessionDao()
        val repository = FocusSessionRepository(dao, nowMillis = { now })
        val id = repository.startSession(plannedMinutes = 5, blockedPackages = listOf("app.a"), startAt = 0)

        now = 5 * 60_000L
        repository.reconcileActiveSessions()

        val stored = dao.sessions.getValue(id)
        assertEquals(5, stored.completedMinutes)
        assertNotNull(stored.endTs)
        assertEquals(FocusSessionState.COMPLETED, FocusSessionEngine.stateOf(stored, now))
    }

    @Test
    fun `reconcileActiveSessions after a large time-skip still finalizes correctly instead of staying stuck`() = runTest {
        var now = 0L
        val dao = FakeFocusSessionDao()
        val repository = FocusSessionRepository(dao, nowMillis = { now })
        val id = repository.startSession(plannedMinutes = 5, blockedPackages = listOf("app.a"), startAt = 0)

        // Simulates the process being dead (killed/rebooted) for an hour past the planned end.
        now = 3_600_000L
        repository.reconcileActiveSessions()

        val stored = dao.sessions.getValue(id)
        assertEquals(5, stored.completedMinutes)
        assertEquals(FocusSessionState.COMPLETED, FocusSessionEngine.stateOf(stored, now))
    }

    @Test
    fun `cancelActiveSessions marks an active session INTERRUPTED`() = runTest {
        var now = 0L
        val dao = FakeFocusSessionDao()
        val repository = FocusSessionRepository(dao, nowMillis = { now })
        val id = repository.startSession(plannedMinutes = 25, blockedPackages = listOf("app.a"), startAt = 0)

        now = 3 * 60_000L
        repository.cancelActiveSessions()

        val stored = dao.sessions.getValue(id)
        assertEquals(3, stored.completedMinutes)
        assertNotNull(stored.endTs)
        assertEquals(FocusSessionState.INTERRUPTED, FocusSessionEngine.stateOf(stored, now))
    }

    @Test
    fun `reconcileActiveSessions does not touch a SCHEDULED future session`() = runTest {
        var now = 0L
        val dao = FakeFocusSessionDao()
        val repository = FocusSessionRepository(dao, nowMillis = { now })
        val id = repository.startSession(plannedMinutes = 25, blockedPackages = listOf("app.a"), startAt = 60_000L)

        repository.reconcileActiveSessions() // now (0) is still before startAt (60_000)

        val stored = dao.sessions.getValue(id)
        assertEquals(0, stored.completedMinutes)
        assertNull(stored.endTs)
        assertEquals(FocusSessionState.SCHEDULED, FocusSessionEngine.stateOf(stored, now))
    }

    @Test
    fun `startSession rejects empty package lists`() = runTest {
        val repository = FocusSessionRepository(FakeFocusSessionDao(), nowMillis = { 0L })

        try {
            repository.startSession(plannedMinutes = 25, blockedPackages = listOf(" ", ""))
            fail("Expected empty package list to be rejected")
        } catch (_: IllegalArgumentException) {
        }
    }

    @Test
    fun `startSession rejects durations outside the supported range`() = runTest {
        val repository = FocusSessionRepository(FakeFocusSessionDao(), nowMillis = { 0L })

        try {
            repository.startSession(plannedMinutes = 0, blockedPackages = listOf("app.a"))
            fail("Expected non-positive duration to be rejected")
        } catch (_: IllegalArgumentException) {
        }
    }

    @Test
    fun `startSession defaults to ALLOW_ALL with no allowed packages when not specified`() = runTest {
        val dao = FakeFocusSessionDao()
        val repository = FocusSessionRepository(dao, nowMillis = { 0L })

        val id = repository.startSession(plannedMinutes = 25, blockedPackages = listOf("app.a"))

        val stored = dao.sessions.getValue(id)
        assertEquals(FocusNotificationPolicy.ALLOW_ALL, FocusNotificationPolicy.fromStored(stored.notificationPolicy))
        assertEquals(emptySet<String>(), stored.allowedNotificationPackages())
    }

    @Test
    fun `startSession persists the chosen notification policy and normalized allowed packages`() = runTest {
        val dao = FakeFocusSessionDao()
        val repository = FocusSessionRepository(dao, nowMillis = { 0L })

        val id = repository.startSession(
            plannedMinutes = 25,
            blockedPackages = listOf("app.a"),
            notificationPolicy = FocusNotificationPolicy.ALLOW_CALLS_AND_SELECTED,
            allowedNotificationPackages = listOf(" app.chat ", "app.chat", "", "app.mail")
        )

        val stored = dao.sessions.getValue(id)
        assertEquals(FocusNotificationPolicy.ALLOW_CALLS_AND_SELECTED, FocusNotificationPolicy.fromStored(stored.notificationPolicy))
        assertEquals(setOf("app.chat", "app.mail"), stored.allowedNotificationPackages())
    }

    @Test
    fun `startSession accepts SILENCE_ALL and ALLOW_CALLS with an empty allowed-package list`() = runTest {
        val dao = FakeFocusSessionDao()
        val repository = FocusSessionRepository(dao, nowMillis = { 0L })

        val silenceId = repository.startSession(plannedMinutes = 25, blockedPackages = listOf("app.a"), notificationPolicy = FocusNotificationPolicy.SILENCE_ALL)
        val callsId = repository.startSession(plannedMinutes = 25, blockedPackages = listOf("app.a"), notificationPolicy = FocusNotificationPolicy.ALLOW_CALLS)

        assertEquals(FocusNotificationPolicy.SILENCE_ALL, FocusNotificationPolicy.fromStored(dao.sessions.getValue(silenceId).notificationPolicy))
        assertEquals(FocusNotificationPolicy.ALLOW_CALLS, FocusNotificationPolicy.fromStored(dao.sessions.getValue(callsId).notificationPolicy))
    }
}
