package com.orlune.app.core.domain.focus

import com.orlune.app.data.local.entity.FocusSessionEntity

/**
 * The four choices from the Focus setup screen. [restrictiveness] is an explicit field
 * (not ordinal) so reordering the enum's declaration can never silently change which
 * policy wins when [FocusNotificationPolicy.effectiveDuring] resolves overlapping
 * sessions. Higher = blocks more.
 *
 * Deliberately does not model "silence everything including alarms" as a distinct
 * choice — [toZenSpec] always keeps alarms audible regardless of policy (see that
 * function's KDoc); an alarm the user set for themselves ringing during Focus is not
 * an "interruption" in the sense this feature is about.
 */
enum class FocusNotificationPolicy(val restrictiveness: Int) {
    ALLOW_ALL(0),
    ALLOW_CALLS_AND_SELECTED(1),
    ALLOW_CALLS(2),
    SILENCE_ALL(3);

    companion object {
        /** Fail-safe per AGENTS.MD: a malformed/unrecognized stored value is treated
         * as the least-restrictive choice, never as silently blocking everything. */
        fun fromStored(value: String): FocusNotificationPolicy = entries.find { it.name == value } ?: ALLOW_ALL
    }
}

/**
 * Platform-independent description of what an [android.service.notification.ZenPolicy]
 * (API 30+) or legacy [android.app.NotificationManager] interruption filter (API 29)
 * should allow through — kept Android-free so the policy-selection logic itself is
 * JVM-unit-testable; `platform/notifications/FocusZenPolicyMapper.kt` is the thin,
 * Android-touching layer that turns this into the real system objects, verified
 * on-device rather than by JVM unit test (see `docs/android-notification-policy.md`).
 */
data class FocusZenSpec(
    val allowCalls: Boolean,
    val allowRepeatCallers: Boolean,
    val allowSelectedApps: Boolean
)

/**
 * Alarms are always left audible — Android's own DND UI draws the same distinction
 * ("Total silence" vs "Alarms only" vs "Priority only"), and silencing a clock alarm
 * the user set for themselves is a materially different, higher-stakes decision than
 * silencing notifications; Focus never makes that call on the user's behalf.
 */
fun FocusNotificationPolicy.toZenSpec(): FocusZenSpec? = when (this) {
    FocusNotificationPolicy.ALLOW_ALL -> null // no Zen rule should be active at all
    FocusNotificationPolicy.SILENCE_ALL -> FocusZenSpec(allowCalls = false, allowRepeatCallers = false, allowSelectedApps = false)
    FocusNotificationPolicy.ALLOW_CALLS -> FocusZenSpec(allowCalls = true, allowRepeatCallers = true, allowSelectedApps = false)
    FocusNotificationPolicy.ALLOW_CALLS_AND_SELECTED -> FocusZenSpec(allowCalls = true, allowRepeatCallers = true, allowSelectedApps = true)
}

/** Parses [FocusSessionEntity.allowedNotificationPackages]; same split/trim/filter
 * shape as [FocusSessionEngine.blockedPackages] for consistency. */
fun FocusSessionEntity.allowedNotificationPackages(): Set<String> =
    allowedNotificationPackages.split(",")
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .toSet()

data class FocusNotificationState(val policy: FocusNotificationPolicy, val allowedPackages: Set<String>)

/**
 * Resolves what Orlune's single system-owned Zen rule should currently enforce, across
 * every [FocusSessionState.ACTIVE] session — deliberately not just the most-recently
 * started one, since overlapping sessions (AGENTS.MD "Known risks": not prevented by
 * the UI, correctly unioned everywhere else in this codebase) must combine the same
 * way blocked-package sets already do. "Most restrictive wins" is resolved by
 * [FocusNotificationPolicy.restrictiveness]; ties keep whichever tied policy appears
 * first, which is safe because same-restrictiveness policies never actually need
 * different Zen configuration except for the allowed-package set. When the effective
 * policy is [FocusNotificationPolicy.ALLOW_CALLS_AND_SELECTED], the allowed-package set
 * is the union of every *currently active* session's own selection at that same
 * restrictiveness — not sessions that are more permissive (e.g. an ALLOW_ALL session
 * running alongside a SILENCE_ALL one contributes nothing, since ALLOW_ALL has no
 * package selection to contribute in the first place).
 *
 * Returns null when no active session wants any restriction — the caller's job is
 * then to deactivate Orlune's Zen rule, never to "restore" anything itself; see
 * `docs/android-notification-policy.md` for why there is nothing else to restore.
 */
fun effectiveFocusNotificationState(sessions: List<FocusSessionEntity>, nowMillis: Long): FocusNotificationState? {
    val active = sessions.filter { FocusSessionEngine.stateOf(it, nowMillis) == FocusSessionState.ACTIVE }
    val policies = active.map { FocusNotificationPolicy.fromStored(it.notificationPolicy) }
    val effectivePolicy = policies.maxByOrNull { it.restrictiveness } ?: return null
    if (effectivePolicy == FocusNotificationPolicy.ALLOW_ALL) return null

    val allowedPackages = if (effectivePolicy == FocusNotificationPolicy.ALLOW_CALLS_AND_SELECTED) {
        active
            .filter { FocusNotificationPolicy.fromStored(it.notificationPolicy) == effectivePolicy }
            .flatMap { it.allowedNotificationPackages() }
            .toSet()
    } else {
        emptySet()
    }
    return FocusNotificationState(effectivePolicy, allowedPackages)
}
