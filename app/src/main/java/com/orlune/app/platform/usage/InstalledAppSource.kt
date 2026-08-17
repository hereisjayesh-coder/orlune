package com.orlune.app.platform.usage

/**
 * Contract for enumerating launchable apps for the app picker. Exists so
 * instrumentation/unit tests can substitute a fake instead of depending on which
 * apps happen to be installed on the test device (same pattern as [AppLabelSource]).
 */
interface InstalledAppSource {
    /** Every launchable app on the device except [excludePackage] (Orlune itself). */
    fun listLaunchableApps(excludePackage: String?): List<InstalledApp>

    /**
     * Live label + icon for a single package, for presenting a package name that
     * came from usage history or a stored rule rather than the app picker (which
     * already only ever shows [listLaunchableApps] results). Returns null if the
     * package is no longer resolvable (e.g. uninstalled) — callers should fall back
     * to a stored label or a generic placeholder, never the raw package name.
     */
    fun resolveDisplayInfo(packageName: String): InstalledApp?

    /**
     * The device's current default home/launcher app's package name, or null if it
     * can't be resolved. Used to present that package as "Home screen" instead of
     * its (often OEM-specific and unfamiliar) launcher app label.
     */
    fun defaultHomePackageName(): String?
}
