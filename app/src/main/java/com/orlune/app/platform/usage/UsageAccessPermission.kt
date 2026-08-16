package com.orlune.app.platform.usage

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Process
import android.provider.Settings

/**
 * Usage Access is "special app access" — never grantable via a runtime permission
 * dialog or manifest declaration alone. The user must manually enable it in Settings;
 * this checks whether they have, via the same AppOpsManager mechanism the platform
 * itself uses (see `docs/android-platform-capabilities.md` row 2).
 */
object UsageAccessPermission {

    // AppOpsManager's checkOpNoThrow <-> unsafeCheckOpNoThrow naming has flipped which
    // one is "current" more than once across API levels; unsafeCheckOpNoThrow is the
    // form confirmed stable and functional since well before minSdk 29, so it's kept
    // deliberately rather than chasing the lint warning onto an unverified API-level
    // assumption about the alternately-recommended name.
    @Suppress("DEPRECATION")
    fun isGranted(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /** Opens the system-wide Usage Access list; there's no reliable per-app deep link. */
    fun settingsIntent(): Intent =
        Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
}
