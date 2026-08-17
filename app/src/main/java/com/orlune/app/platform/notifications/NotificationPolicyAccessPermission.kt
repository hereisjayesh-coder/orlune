package com.orlune.app.platform.notifications

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.provider.Settings

/**
 * "Notification Policy Access" / "Do Not Disturb access" — special app access, same
 * manual-Settings-grant shape as
 * [UsageAccessPermission][com.orlune.app.platform.usage.UsageAccessPermission] and
 * [OverlayPermission][com.orlune.app.platform.blocking.OverlayPermission]. Gates every
 * `AutomaticZenRule`/`NotificationManager.setAutomaticZenRuleState` call in
 * `FocusZenRuleController` — without it those throw `SecurityException`, always
 * caught there (fail-safe: no crash, no notification silencing, exactly like a
 * missing Usage Access permission fails open rather than blocking).
 *
 * Deliberately a distinct class from
 * [NotificationPermission][com.orlune.app.platform.blocking.NotificationPermission],
 * which checks the unrelated `POST_NOTIFICATIONS` runtime permission (whether Orlune
 * itself may show its own foreground-service notification) — same Android subsystem
 * area, different permission, different Settings screen, easy to confuse by name.
 */
object NotificationPolicyAccessPermission {

    fun isGranted(context: Context): Boolean {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return manager.isNotificationPolicyAccessGranted
    }

    /** No per-app deep link exists; this opens the system-wide list, same as
     * [UsageAccessPermission.settingsIntent][com.orlune.app.platform.usage.UsageAccessPermission.settingsIntent]. */
    fun settingsIntent(): Intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
}
