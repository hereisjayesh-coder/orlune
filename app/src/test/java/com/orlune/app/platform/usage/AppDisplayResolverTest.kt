package com.orlune.app.platform.usage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Hand-rolled fake — no mocking library in this project, see AGENTS.MD conventions. */
private class FakeInstalledAppSource(
    private val resolvable: Map<String, InstalledApp>,
    private val homePackageName: String?
) : InstalledAppSource {
    override fun listLaunchableApps(excludePackage: String?): List<InstalledApp> =
        resolvable.values.filter { it.packageName != excludePackage }

    override fun resolveDisplayInfo(packageName: String): InstalledApp? = resolvable[packageName]

    override fun defaultHomePackageName(): String? = homePackageName
}

class AppDisplayResolverTest {

    @Test
    fun `resolves live label and icon when the package is currently resolvable`() {
        val source = FakeInstalledAppSource(
            resolvable = mapOf("com.example.chat" to InstalledApp("com.example.chat", "Chat", null)),
            homePackageName = null
        )
        val resolver = AppDisplayResolver(source)

        val result = resolver.resolve("com.example.chat", storedLabel = "Stale Old Name")

        assertEquals("Chat", result.label)
    }

    @Test
    fun `falls back to the stored label when live resolution fails`() {
        val source = FakeInstalledAppSource(resolvable = emptyMap(), homePackageName = null)
        val resolver = AppDisplayResolver(source)

        val result = resolver.resolve("com.example.uninstalled", storedLabel = "Previously Known App")

        assertEquals("Previously Known App", result.label)
    }

    @Test
    fun `never falls back to the raw package name even when it was stored as the label`() {
        val source = FakeInstalledAppSource(resolvable = emptyMap(), homePackageName = null)
        val resolver = AppDisplayResolver(source)

        val result = resolver.resolve("com.example.uninstalled", storedLabel = "com.example.uninstalled")

        assertEquals("Unknown app", result.label)
    }

    @Test
    fun `falls back to a generic label when nothing is resolvable and no stored label exists`() {
        val source = FakeInstalledAppSource(resolvable = emptyMap(), homePackageName = null)
        val resolver = AppDisplayResolver(source)

        val result = resolver.resolve("com.example.unknown", storedLabel = null)

        assertEquals("Unknown app", result.label)
    }

    @Test
    fun `blank stored label is treated as absent, not shown verbatim`() {
        val source = FakeInstalledAppSource(resolvable = emptyMap(), homePackageName = null)
        val resolver = AppDisplayResolver(source)

        val result = resolver.resolve("com.example.unknown", storedLabel = "   ")

        assertEquals("Unknown app", result.label)
    }

    @Test
    fun `the current default home launcher package is presented as Home screen`() {
        val source = FakeInstalledAppSource(
            resolvable = mapOf(
                "com.google.android.apps.nexuslauncher" to InstalledApp(
                    "com.google.android.apps.nexuslauncher", "Nexus Launcher", null
                )
            ),
            homePackageName = "com.google.android.apps.nexuslauncher"
        )
        val resolver = AppDisplayResolver(source)

        val result = resolver.resolve("com.google.android.apps.nexuslauncher", storedLabel = null)

        assertEquals("Home screen", result.label)
    }

    @Test
    fun `the home package is labeled Home screen even if it cannot currently be resolved`() {
        val source = FakeInstalledAppSource(
            resolvable = emptyMap(),
            homePackageName = "com.google.android.apps.nexuslauncher"
        )
        val resolver = AppDisplayResolver(source)

        val result = resolver.resolve("com.google.android.apps.nexuslauncher", storedLabel = "com.google.android.apps.nexuslauncher")

        assertEquals("Home screen", result.label)
        assertNull(result.icon)
    }

    @Test
    fun `a non-home package is unaffected by an unrelated home package being set`() {
        val source = FakeInstalledAppSource(
            resolvable = mapOf("com.example.chat" to InstalledApp("com.example.chat", "Chat", null)),
            homePackageName = "com.google.android.apps.nexuslauncher"
        )
        val resolver = AppDisplayResolver(source)

        val result = resolver.resolve("com.example.chat", storedLabel = null)

        assertEquals("Chat", result.label)
    }
}
