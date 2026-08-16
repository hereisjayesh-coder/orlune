package com.orlune.app.core.domain.rules

import com.orlune.app.data.local.entity.AppListEntryEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class BlockingEngineTest {

    @Test
    fun `no list entry and no rule triggered allows`() {
        val decision = BlockingEngine.decide("app.a", emptyList(), anyRuleTriggered = false)

        assertEquals(BlockDecision.ALLOW, decision)
    }

    @Test
    fun `no list entry and a triggered rule blocks`() {
        val decision = BlockingEngine.decide("app.a", emptyList(), anyRuleTriggered = true)

        assertEquals(BlockDecision.BLOCK, decision)
    }

    @Test
    fun `explicit allow entry wins even when a rule is triggered`() {
        val entries = listOf(AppListEntryEntity(packageName = "app.a", listType = "allow"))

        val decision = BlockingEngine.decide("app.a", entries, anyRuleTriggered = true)

        assertEquals(BlockDecision.ALLOW, decision)
    }

    @Test
    fun `explicit block entry wins even when no rule is triggered`() {
        val entries = listOf(AppListEntryEntity(packageName = "app.a", listType = "block"))

        val decision = BlockingEngine.decide("app.a", entries, anyRuleTriggered = false)

        assertEquals(BlockDecision.BLOCK, decision)
    }

    @Test
    fun `list entries for other packages are ignored`() {
        val entries = listOf(AppListEntryEntity(packageName = "app.other", listType = "allow"))

        val decision = BlockingEngine.decide("app.a", entries, anyRuleTriggered = true)

        assertEquals(BlockDecision.BLOCK, decision)
    }

    @Test
    fun `unrecognized list type falls back to rule-triggered decision`() {
        val entries = listOf(AppListEntryEntity(packageName = "app.a", listType = "mystery"))

        val decision = BlockingEngine.decide("app.a", entries, anyRuleTriggered = true)

        assertEquals(BlockDecision.BLOCK, decision)
    }

    @Test
    fun `listType maps raw strings to AppListType`() {
        assertEquals(AppListType.BLOCK, AppListEntryEntity(packageName = "app.a", listType = "block").listType())
        assertEquals(AppListType.ALLOW, AppListEntryEntity(packageName = "app.a", listType = "allow").listType())
        assertEquals(null, AppListEntryEntity(packageName = "app.a", listType = "nonsense").listType())
    }
}
