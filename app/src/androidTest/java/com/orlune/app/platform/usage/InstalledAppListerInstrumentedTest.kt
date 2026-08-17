package com.orlune.app.platform.usage

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Exercises the real PackageManager-backed app picker data source on-device — this
 * is exactly the package -> label and package -> icon mapping the app picker
 * depends on, per AGENTS.MD's "never expose package names to ordinary users" rule.
 */
@RunWith(AndroidJUnit4::class)
class InstalledAppListerInstrumentedTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val lister = InstalledAppLister(context)

    @Test
    fun listLaunchableApps_returnsAtLeastOneRealLaunchableApp() {
        // The test device always has at least a launcher and Orlune's own debug/test
        // packages installed, so this must be non-empty on any real device or emulator.
        val apps = lister.listLaunchableApps(excludePackage = null)
        assertTrue(apps.isNotEmpty())
    }

    @Test
    fun listLaunchableApps_everyEntryHasANonBlankLabel() {
        // This is the concrete guarantee behind "never expose package names to
        // ordinary users" — every row the picker can show resolves to some label,
        // never an empty string standing in for one.
        val apps = lister.listLaunchableApps(excludePackage = null)
        assertTrue(apps.all { it.label.isNotBlank() })
    }

    @Test
    fun listLaunchableApps_excludesTheGivenPackage() {
        val allApps = lister.listLaunchableApps(excludePackage = null)
        val ownPackage = context.packageName
        assertTrue("test setup assumption: Orlune's own package must be launchable", allApps.any { it.packageName == ownPackage })

        val withoutSelf = lister.listLaunchableApps(excludePackage = ownPackage)
        assertFalse(withoutSelf.any { it.packageName == ownPackage })
        assertEquals(allApps.size - 1, withoutSelf.size)
    }

    @Test
    fun listLaunchableApps_isSortedAlphabeticallyByLabelCaseInsensitive() {
        val apps = lister.listLaunchableApps(excludePackage = null)
        val labels = apps.map { it.label.lowercase() }
        assertEquals(labels.sorted(), labels)
    }

    @Test
    fun listLaunchableApps_hasNoDuplicatePackageNames() {
        val apps = lister.listLaunchableApps(excludePackage = null)
        assertEquals(apps.size, apps.map { it.packageName }.distinct().size)
    }

    @Test
    fun resolveDisplayInfo_resolvesOrlunesOwnPackage() {
        // Orlune's own package is always installed on the test device running this
        // test, so it must resolve to a non-blank label via the same code path
        // listLaunchableApps uses internally.
        val result = lister.resolveDisplayInfo(context.packageName)
        assertTrue(result != null && result.label.isNotBlank())
    }

    @Test
    fun resolveDisplayInfo_returnsNullForAnObviouslyNonexistentPackage() {
        val result = lister.resolveDisplayInfo("com.orlune.app.this.package.does.not.exist.test.only")
        assertEquals(null, result)
    }

    @Test
    fun defaultHomePackageName_resolvesToARealLaunchableApp() {
        // Every real device/emulator this suite runs on has a default launcher set,
        // so this must resolve to some package — and per Android's package-visibility
        // model, that package must also be discoverable as launchable (this is
        // exactly the manifest <queries> CATEGORY_HOME entry this test exists to
        // guard: without it, this call returns null even though a default launcher
        // is set on the device).
        val homePackage = lister.defaultHomePackageName()
        assertTrue("expected a resolvable default launcher package on the test device", homePackage != null)
        val allApps = lister.listLaunchableApps(excludePackage = null)
        assertTrue(
            "the default launcher package should itself be resolvable via PackageManager",
            lister.resolveDisplayInfo(homePackage!!) != null || allApps.any { it.packageName == homePackage }
        )
    }
}
