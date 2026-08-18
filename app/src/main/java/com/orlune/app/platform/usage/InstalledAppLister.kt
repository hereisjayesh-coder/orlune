package com.orlune.app.platform.usage

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build
import android.util.LruCache
import androidx.core.graphics.drawable.toBitmap

/**
 * Enumerates launchable apps via the `<queries>` `CATEGORY_LAUNCHER` filter already
 * declared in AndroidManifest.xml — deliberately not `QUERY_ALL_PACKAGES` (see
 * docs/app-visibility-compliance.md). This is the same scoped mechanism
 * [AppLabelResolver] already relies on for single-package label lookups; this class
 * enumerates the whole launchable set for the app picker.
 *
 * One instance lives for the process's whole lifetime ([com.orlune.app.OrluneApplication.installedAppLister]
 * is a `by lazy` val), so [cache] is a real cross-screen cache, not a per-composition
 * one: Home, Insights, Limits, Focus, the app picker, and
 * [com.orlune.app.platform.blocking.BlockingMonitorService] (which re-resolves the
 * blocked app's icon on every 3-second tick while a block screen is showing) all share
 * it, so the same package's icon is only ever decoded from PackageManager once per
 * process, not once per screen visit or once per tick.
 */
class InstalledAppLister(private val context: Context) : InstalledAppSource {

    /** Bounded by entry count so a device with an unusually large app catalog can't
     * grow this without limit — 200 comfortably covers a typical launchable-app count
     * (with the label + a decoded icon Bitmap each) while still bounding worst-case
     * memory. A resolved [InstalledApp] never changes for a given [packageName] within
     * a process's lifetime without an app update, which restarts this process anyway. */
    private val cache = LruCache<String, InstalledApp>(200)

    override fun listLaunchableApps(excludePackage: String?): List<InstalledApp> {
        val packageManager = context.packageManager
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolveInfos: List<ResolveInfo> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.queryIntentActivities(launcherIntent, PackageManager.ResolveInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            packageManager.queryIntentActivities(launcherIntent, 0)
        }

        return resolveInfos
            .map { it.activityInfo.packageName }
            .distinct()
            .filter { it != excludePackage }
            .mapNotNull { packageName -> resolveApp(packageName) }
            .sortedBy { it.label.lowercase() }
    }

    override fun resolveDisplayInfo(packageName: String): InstalledApp? = resolveApp(packageName)

    override fun defaultHomePackageName(): String? {
        val packageManager = context.packageManager
        val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val resolveInfo = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.resolveActivity(homeIntent, PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong()))
            } else {
                @Suppress("DEPRECATION")
                packageManager.resolveActivity(homeIntent, PackageManager.MATCH_DEFAULT_ONLY)
            }
        }.getOrNull()
        return resolveInfo?.activityInfo?.packageName
    }

    private fun resolveApp(packageName: String): InstalledApp? {
        cache.get(packageName)?.let { return it }
        val packageManager = context.packageManager
        val resolved = runCatching {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            InstalledApp(
                packageName = packageName,
                label = packageManager.getApplicationLabel(appInfo).toString(),
                icon = runCatching { packageManager.getApplicationIcon(packageName).toBitmap() }.getOrNull()
            )
        }.getOrNull()
        if (resolved != null) cache.put(packageName, resolved)
        return resolved
    }
}
