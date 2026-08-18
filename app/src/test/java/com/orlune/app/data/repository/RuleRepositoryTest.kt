package com.orlune.app.data.repository

import com.orlune.app.data.local.dao.RuleDao
import com.orlune.app.data.local.dao.RuleSnoozeDao
import com.orlune.app.data.local.dao.ScheduleDao
import com.orlune.app.data.local.entity.RuleEntity
import com.orlune.app.data.local.entity.RuleSnoozeEntity
import com.orlune.app.data.local.entity.ScheduleEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RuleRepositoryTest {

    private class FakeRuleDao : RuleDao {
        val upserted = mutableListOf<RuleEntity>()
        val deleted = mutableListOf<RuleEntity>()
        override suspend fun upsert(rule: RuleEntity): Long {
            upserted.add(rule)
            return upserted.size.toLong()
        }
        override suspend fun delete(rule: RuleEntity) { deleted.add(rule) }
        override fun observeAll(): Flow<List<RuleEntity>> = flowOf(upserted)
    }

    private class FakeScheduleDao : ScheduleDao {
        val upserted = mutableListOf<ScheduleEntity>()
        override suspend fun upsert(schedule: ScheduleEntity): Long {
            upserted.add(schedule)
            return upserted.size.toLong()
        }
        override suspend fun delete(schedule: ScheduleEntity) {}
        override fun observeForRule(ruleId: Long): Flow<List<ScheduleEntity>> =
            flowOf(upserted.filter { it.associatedRuleId == ruleId })
    }

    private class FakeRuleSnoozeDao : RuleSnoozeDao {
        val upserted = mutableListOf<RuleSnoozeEntity>()
        override suspend fun upsert(snooze: RuleSnoozeEntity) { upserted.add(snooze) }
        override suspend fun get(packageName: String): RuleSnoozeEntity? = upserted.lastOrNull { it.packageName == packageName }
        override suspend fun delete(packageName: String) { upserted.removeAll { it.packageName == packageName } }
        override fun observeAll(): Flow<List<RuleSnoozeEntity>> = flowOf(upserted)
    }

    private fun repository(
        ruleDao: FakeRuleDao = FakeRuleDao(),
        scheduleDao: FakeScheduleDao = FakeScheduleDao(),
        ruleSnoozeDao: FakeRuleSnoozeDao = FakeRuleSnoozeDao()
    ) = Triple(RuleRepository(ruleDao, scheduleDao, ruleSnoozeDao), ruleDao, ruleSnoozeDao)

    @Test
    fun `addDailyLimit persists a limit rule with the given threshold`() = runTest {
        val (repo, ruleDao) = repository()
        repo.addDailyLimit("com.example.social", 1200L)
        assertEquals(1, ruleDao.upserted.size)
        assertEquals("limit", ruleDao.upserted[0].type)
        assertEquals("com.example.social", ruleDao.upserted[0].targetPackageOrCategory)
        assertEquals(1200L, ruleDao.upserted[0].threshold)
    }

    @Test
    fun `addDailyLimit rejects a zero threshold`() = runTest {
        val (repo, ruleDao) = repository()
        val threw = try {
            repo.addDailyLimit("com.example.social", 0L)
            false
        } catch (e: IllegalArgumentException) {
            true
        }
        assertTrue(threw)
        assertTrue(ruleDao.upserted.isEmpty())
    }

    @Test
    fun `addDailyLimit rejects a threshold beyond 24 hours`() = runTest {
        val (repo, _) = repository()
        val threw = try {
            repo.addDailyLimit("com.example.social", 86_401L)
            false
        } catch (e: IllegalArgumentException) {
            true
        }
        assertTrue(threw)
    }

    @Test
    fun `addDailyLimit rejects a blank package name`() = runTest {
        val (repo, _) = repository()
        val threw = try {
            repo.addDailyLimit("  ", 1200L)
            false
        } catch (e: IllegalArgumentException) {
            true
        }
        assertTrue(threw)
    }

    @Test
    fun `addSchedule creates a schedule rule and its schedule row linked by id`() = runTest {
        val ruleDao = FakeRuleDao()
        val scheduleDao = FakeScheduleDao()
        val repo = RuleRepository(ruleDao, scheduleDao, FakeRuleSnoozeDao())
        val id = repo.addSchedule("Work hours", "com.example.social", "MON,TUE", "09:00", "17:00")
        assertEquals("schedule", ruleDao.upserted[0].type)
        assertEquals(1, scheduleDao.upserted.size)
        assertEquals(id, scheduleDao.upserted[0].associatedRuleId)
        assertEquals("Work hours", scheduleDao.upserted[0].name)
    }

    @Test
    fun `delete forwards to the DAO`() = runTest {
        val (repo, ruleDao) = repository()
        val rule = RuleEntity(id = 1, type = "limit", targetPackageOrCategory = "com.example.social", threshold = 1200L, windowDefinition = null)
        repo.delete(rule)
        assertEquals(listOf(rule), ruleDao.deleted)
    }

    @Test
    fun `snooze upserts a snoozedUntil computed from minutes and now`() = runTest {
        val (repo, _, snoozeDao) = repository()
        repo.snooze("com.example.social", 10, nowMillis = 1_000L)
        val snooze = snoozeDao.get("com.example.social")
        assertEquals(1_000L + 10 * 60_000L, snooze?.snoozedUntil)
    }

    @Test
    fun `repeated snoozes replace the prior snooze rather than accumulating`() = runTest {
        val (repo, _, snoozeDao) = repository()
        repo.snooze("com.example.social", 10, nowMillis = 1_000L)
        repo.snooze("com.example.social", 30, nowMillis = 5_000L)
        val snooze = snoozeDao.get("com.example.social")
        assertEquals(5_000L + 30 * 60_000L, snooze?.snoozedUntil)
    }

    @Test
    fun `snooze rejects zero minutes`() = runTest {
        val (repo, _, _) = repository()
        val threw = try {
            repo.snooze("com.example.social", 0)
            false
        } catch (e: IllegalArgumentException) {
            true
        }
        assertTrue(threw)
    }

    @Test
    fun `snooze rejects more than the maximum`() = runTest {
        val (repo, _, _) = repository()
        val threw = try {
            repo.snooze("com.example.social", RuleRepository.MAX_SNOOZE_MINUTES + 1)
            false
        } catch (e: IllegalArgumentException) {
            true
        }
        assertTrue(threw)
    }

    @Test
    fun `snoozing one package leaves others untouched`() = runTest {
        val (repo, _, snoozeDao) = repository()
        repo.snooze("com.example.social", 10, nowMillis = 1_000L)
        assertNull(snoozeDao.get("com.example.other"))
    }
}
