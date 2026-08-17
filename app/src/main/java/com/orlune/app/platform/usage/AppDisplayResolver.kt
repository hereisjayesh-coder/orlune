package com.orlune.app.platform.usage

/**
 * Presentation-only label/icon resolution for a package name already known from
 * usage history or a stored rule. Re-resolves live from [InstalledAppSource] rather
 * than trusting a possibly-stale stored label (labels are only ever written once,
 * see UsageRepository.ensureAppsKnown), and special-cases the device's current
 * default launcher so it reads as "Home screen" rather than an OEM-specific launcher
 * app label a typical user won't recognize (e.g. "Nexus Launcher"). Never falls back
 * to a raw package name — an unresolvable package with no usable stored label
 * becomes "Unknown app" instead.
 *
 * One instance per screen load: [defaultHomePackageName] issues a PackageManager
 * query, so it's resolved once and cached for every row that instance resolves.
 */
class AppDisplayResolver(private val source: InstalledAppSource) {
    private val homePackageName: String? by lazy { source.defaultHomePackageName() }

    fun resolve(packageName: String, storedLabel: String?): AppDisplayInfo {
        val live = source.resolveDisplayInfo(packageName)
        if (packageName == homePackageName) {
            return AppDisplayInfo(packageName, "Home screen", live?.icon)
        }
        val label = live?.label
            ?: storedLabel?.takeIf { it.isNotBlank() && it != packageName }
            ?: "Unknown app"
        return AppDisplayInfo(packageName, label, live?.icon)
    }
}
