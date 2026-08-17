package com.orlune.app.platform.notifications

import android.app.AutomaticZenRule
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.service.notification.Condition
import android.util.Log
import com.orlune.app.MainActivity
import com.orlune.app.core.domain.focus.FocusNotificationPolicy
import com.orlune.app.core.domain.focus.FocusZenSpec
import com.orlune.app.core.domain.focus.toZenSpec

/**
 * Owns exactly one system-registered `AutomaticZenRule`, named "Orlune Focus" and
 * identified by [conditionId] — never touches
 * [NotificationManager.setInterruptionFilter]/`setNotificationPolicy`, the older
 * whole-device APIs, at all. See `docs/android-notification-policy.md` for the full
 * reasoning; short version: Android's Zen Mode already combines every currently-active
 * rule (the user's own manual DND, Bedtime, other apps' rules, this one) into one
 * "most restrictive wins" effective filter on its own. Orlune's only job is to keep
 * its own rule's `Condition` (`STATE_TRUE`/`STATE_FALSE`) in sync with whether a Focus
 * session currently wants it active — never to read, save, or restore anyone else's
 * state, and never to delete/recreate the rule itself once it exists (the rule stays
 * registered but inactive between sessions, exactly like "Bedtime" or "Driving" stay
 * visible-but-off in system Settings between uses).
 *
 * Every public function fails safe: missing permission or any `SecurityException` is
 * logged and swallowed, never crashes the caller (`BlockingMonitorService`'s tick is
 * already wrapped in its own try/catch, but this controller doesn't rely on that).
 */
object FocusZenRuleController {
    private const val TAG = "FocusZenRuleController"
    private const val RULE_NAME = "Orlune Focus"

    private fun conditionId(context: Context): Uri = Condition.newId(context).appendPath("focus").build()

    /**
     * Called every [com.orlune.app.platform.blocking.BlockingMonitorService] tick (and
     * therefore recovers on its own after an app restart, process death, or device
     * reboot — no separate persisted "is my rule currently on" flag to get stale) with
     * [desired] = `null` when no active session wants any restriction. Idempotent:
     * calling this repeatedly with the same [desired] is safe and cheap enough to run
     * every tick without special-casing "did anything change".
     */
    fun reconcile(context: Context, desired: FocusNotificationPolicy?) {
        if (!NotificationPolicyAccessPermission.isGranted(context)) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        val spec = desired?.toZenSpec()
        try {
            if (spec == null) {
                deactivate(context, manager)
            } else {
                val ruleId = ensureRuleRegistered(context, manager, spec)
                manager.setAutomaticZenRuleState(
                    ruleId,
                    Condition(conditionId(context), "Focus is active", Condition.STATE_TRUE)
                )
            }
        } catch (e: SecurityException) {
            // Permission revoked between the isGranted() check above and this call
            // (e.g. the user revoked it in Settings mid-tick) — fail safe, not crash.
            Log.w(TAG, "Notification policy access unavailable, skipping this tick", e)
        }
    }

    private fun deactivate(context: Context, manager: NotificationManager) {
        val existing = findExistingRule(context, manager) ?: return
        manager.setAutomaticZenRuleState(
            existing.key,
            Condition(conditionId(context), "Focus has ended", Condition.STATE_FALSE)
        )
    }

    private fun findExistingRule(context: Context, manager: NotificationManager): Map.Entry<String, AutomaticZenRule>? =
        runCatching { manager.automaticZenRules }
            .getOrNull()
            ?.entries
            ?.firstOrNull { it.value.conditionId == conditionId(context) }

    // The 5-arg constructor is deprecated in favor of the ZenPolicy-accepting one, but
    // it's still the only constructor available on API 29 (this project's minSdk) — see
    // FocusZenPolicyMapper's KDoc for why both branches are needed.
    @Suppress("DEPRECATION")
    private fun ensureRuleRegistered(context: Context, manager: NotificationManager, spec: FocusZenSpec): String {
        val existing = findExistingRule(context, manager)
        // No ConditionProviderService is declared or needed: this app manages the
        // rule's Condition itself via setAutomaticZenRuleState (available since API 29,
        // added specifically to support this "manual" pattern) rather than a bound
        // service reporting conditions on a schedule. Confirmed on-device (API 37):
        // the platform rejects a rule with neither a real ConditionProviderService NOR
        // a configurationActivity ("Rule must have a valid (enabled) ConditionProviderService
        // or configurationActivity", IllegalArgumentException from addAutomaticZenRule) —
        // an arbitrary non-CPS `owner` alone is not sufficient on this OS version, contrary
        // to some older documentation/examples. MainActivity satisfies the check as
        // `configurationActivity` (it's already exported) without adding a real
        // ConditionProviderService or a dedicated settings sub-screen for it.
        val mainActivity = ComponentName(context, MainActivity::class.java)
        val rule = if (FocusZenPolicyMapper.supportsZenPolicy()) {
            AutomaticZenRule(
                RULE_NAME,
                /* owner = */ null,
                /* configurationActivity = */ mainActivity,
                conditionId(context),
                FocusZenPolicyMapper.toAndroidZenPolicy(spec),
                NotificationManager.INTERRUPTION_FILTER_PRIORITY,
                /* enabled = */ true
            )
        } else {
            // The legacy (API 29) 5-arg constructor has no configurationActivity
            // parameter — only `owner` — so it's passed here even though its
            // ZenPolicy-constructor sibling above needs configurationActivity instead.
            // Not independently verified on real API 29 hardware (this project's test
            // device is API 37); see docs/android-notification-policy.md.
            AutomaticZenRule(RULE_NAME, mainActivity, conditionId(context), FocusZenPolicyMapper.toLegacyInterruptionFilter(spec), true)
        }
        return if (existing != null) {
            manager.updateAutomaticZenRule(existing.key, rule)
            existing.key
        } else {
            manager.addAutomaticZenRule(rule)
        }
    }
}
