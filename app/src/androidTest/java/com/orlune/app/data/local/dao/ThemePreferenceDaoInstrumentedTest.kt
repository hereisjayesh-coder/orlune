package com.orlune.app.data.local.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.orlune.app.data.local.OrluneDatabase
import com.orlune.app.data.local.entity.ThemePreferenceEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Verifies the Settings > Appearance choice (System/Light/Dark) actually round-trips
 * through Room — this is what "persist the user's choice locally" and "survive app
 * restart" rest on, since OrluneTheme just reads whatever this DAO last observed.
 */
@RunWith(AndroidJUnit4::class)
class ThemePreferenceDaoInstrumentedTest {

    private lateinit var database: OrluneDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, OrluneDatabase::class.java).build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun observe_withNoStoredPreference_emitsNull() = runTest {
        assertNull(database.themePreferenceDao().observe().first())
    }

    @Test
    fun upsert_thenObserve_returnsTheStoredThemeId() = runTest {
        database.themePreferenceDao().upsert(ThemePreferenceEntity(themeId = "dark"))

        assertEquals("dark", database.themePreferenceDao().observe().first()?.themeId)
    }

    @Test
    fun upsert_twice_overwritesRatherThanAddingASecondRow() = runTest {
        val dao = database.themePreferenceDao()
        dao.upsert(ThemePreferenceEntity(themeId = "light"))
        dao.upsert(ThemePreferenceEntity(themeId = "dark"))

        assertEquals("dark", dao.observe().first()?.themeId)
    }

    @Test
    fun upsert_supportsAllThreeAppearanceModes() = runTest {
        val dao = database.themePreferenceDao()
        for (mode in listOf("system", "light", "dark")) {
            dao.upsert(ThemePreferenceEntity(themeId = mode))
            assertEquals(mode, dao.observe().first()?.themeId)
        }
    }
}
