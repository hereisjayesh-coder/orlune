package com.orlune.app.platform.usage

import android.content.Context
import android.content.pm.PackageManager

/**
 * Resolves a package's display label via PackageManager, scoped by the `<queries>`
 * `CATEGORY_LAUNCHER` filter in AndroidManifest.xml (deliberately not
 * `QUERY_ALL_PACKAGES` — Section 12.3). Falls back to the raw package name for
 * anything not resolvable (e.g. a background/system component with no launcher
 * activity) rather than crashing or silently dropping the app from usage data.
 */
class AppLabelResolver(private val context: Context) : AppLabelSource {

    override fun resolveLabel(packageName: String): String {
        val packageManager = context.packageManager
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            packageName
        }
    }
}
