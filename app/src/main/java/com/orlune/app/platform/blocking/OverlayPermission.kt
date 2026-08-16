package com.orlune.app.platform.blocking

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

/**
 * "Display over other apps" — special app access, same manual-Settings-grant shape as
 * [UsageAccessPermission][com.orlune.app.platform.usage.UsageAccessPermission]. Required
 * to draw the blocking overlay; checked fresh before every draw attempt since the user
 * can revoke it at any time from Settings, outside the app.
 */
object OverlayPermission {

    fun isGranted(context: Context): Boolean = Settings.canDrawOverlays(context)

    fun settingsIntent(context: Context): Intent =
        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
}
