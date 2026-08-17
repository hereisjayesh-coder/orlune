package com.orlune.app.platform.usage

/**
 * Contract for enumerating launchable apps for the app picker. Exists so
 * instrumentation/unit tests can substitute a fake instead of depending on which
 * apps happen to be installed on the test device (same pattern as [AppLabelSource]).
 */
interface InstalledAppSource {
    /** Every launchable app on the device except [excludePackage] (Orlune itself). */
    fun listLaunchableApps(excludePackage: String?): List<InstalledApp>
}
