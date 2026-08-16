package com.orlune.app.platform.blocking

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * `POST_NOTIFICATIONS` (API 33+ runtime permission) — requested for UX only, not
 * correctness: [BlockingMonitorService] keeps running as a foreground service even if
 * this is denied, since Android does not kill a foreground service for lacking
 * notification permission, it just won't display the persistent notification.
 */
object NotificationPermission {

    fun isGranted(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }
}
