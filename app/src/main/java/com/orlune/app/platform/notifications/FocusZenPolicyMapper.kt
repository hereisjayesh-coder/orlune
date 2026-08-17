package com.orlune.app.platform.notifications

import android.app.NotificationManager
import android.os.Build
import android.service.notification.ZenPolicy
import com.orlune.app.core.domain.focus.FocusZenSpec

/**
 * Turns the Android-free [FocusZenSpec] into the real system objects. Split out from
 * `core/domain/focus/FocusNotificationPolicy.kt` because `android.service.notification.ZenPolicy`
 * is a real Android framework class this project's JVM unit tests can't construct (no
 * Robolectric — see AGENTS.MD conventions); [FocusZenSpec] itself is what's actually
 * unit-tested, this mapping is verified on-device instead (see
 * `docs/android-notification-policy.md`).
 *
 * Two branches, confirmed against this project's real compileSdk 37 `android.jar`
 * (`javap`/decompiled stub source, not memory) before writing this:
 * - API 30 (R)+: `AutomaticZenRule`'s `ZenPolicy`-accepting constructor exists, so every
 *   category is controlled individually via [toAndroidZenPolicy].
 * - API 29 (Q, this project's `minSdk`): that constructor does not exist yet — only the
 *   4-value `NotificationManager.INTERRUPTION_FILTER_*` constructor is available, so
 *   [toLegacyInterruptionFilter] is the best available fidelity: `ALARMS` (silence
 *   everything but the device's own alarms) for [FocusZenSpec.allowCalls] == false, or
 *   `PRIORITY` (whatever the device's own Settings > Sound > Do Not Disturb > "Priority
 *   only allows" list already says) when calls should get through — Orlune cannot
 *   independently define "calls only" on API 29 the way it can on API 30+.
 */
object FocusZenPolicyMapper {

    fun supportsZenPolicy(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R

    fun toAndroidZenPolicy(spec: FocusZenSpec): ZenPolicy {
        val builder = ZenPolicy.Builder()
            .allowAlarms(true)
            .allowMedia(false)
            .allowSystem(false)
            .allowReminders(false)
            .allowEvents(false)
            .allowRepeatCallers(spec.allowRepeatCallers)
            .allowCalls(if (spec.allowCalls) ZenPolicy.PEOPLE_TYPE_ANYONE else ZenPolicy.PEOPLE_TYPE_NONE)
            .allowMessages(ZenPolicy.PEOPLE_TYPE_NONE)
            .allowConversations(if (spec.allowSelectedApps) ZenPolicy.CONVERSATION_SENDERS_IMPORTANT else ZenPolicy.CONVERSATION_SENDERS_NONE)
        // allowPriorityChannels: documented from API 31 (S) onward; guarded rather than
        // asserted from the constant's mere presence in the compileSdk 37 stub jar,
        // which lists every method regardless of the API level it was actually added at
        // (see the file-level KDoc). Omitting it on 30 leaves the builder default,
        // which is fail-safe (more restrictive, never less) if this guard is ever wrong.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.allowPriorityChannels(spec.allowSelectedApps)
        }
        return builder.build()
    }

    fun toLegacyInterruptionFilter(spec: FocusZenSpec): Int =
        if (spec.allowCalls) NotificationManager.INTERRUPTION_FILTER_PRIORITY else NotificationManager.INTERRUPTION_FILTER_ALARMS
}
