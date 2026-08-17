package com.orlune.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A user-initiated focus period. [blockedCategoryIds] is a comma-separated list of
 * [AppCategoryEntity] ids — reserved for real category-based blocking once some
 * later phase actually builds a way to assign apps to categories (nothing does yet,
 * `AppCategoryEntity` has zero rows anywhere in this codebase); left unpopulated by
 * Phase 6. [blockedPackages] (Phase 6) is a comma-separated list of package names —
 * the actual, tested blocking mechanism for focus sessions.
 *
 * [notificationPolicy] (Phase 8, notification/quiet mode) is a
 * [com.orlune.app.core.domain.focus.FocusNotificationPolicy] enum name, resolved
 * fail-safe via `FocusNotificationPolicy.fromStored`. [allowedNotificationPackages]
 * is a comma-separated package list, only meaningful when [notificationPolicy] is
 * `ALLOW_CALLS_AND_SELECTED`; see `docs/android-notification-policy.md` for what this
 * selection actually controls (Orlune cannot mark another app's channel as
 * DND-priority on the user's behalf — the platform doesn't expose that).
 *
 * No stored status column: a session's state ([com.orlune.app.core.domain.focus.FocusSessionEngine.stateOf])
 * is a pure function of [startTs]/[endTs]/[plannedMinutes]/[completedMinutes] and the
 * current time, so a crash/reboot/restart can never leave a session in a state that
 * doesn't reconcile correctly once something reads it again. The same applies to
 * notification-policy enforcement: nothing here records "is Orlune's Zen rule
 * currently on" — that's re-derived from these columns plus the live system Zen-rule
 * state every time, never trusted as stale stored fact (see
 * `core/domain/focus/FocusNotificationPolicy.kt`'s `effectiveFocusNotificationState`).
 */
@Entity(tableName = "focus_sessions")
data class FocusSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startTs: Long,
    val endTs: Long?,
    val plannedMinutes: Int,
    val completedMinutes: Int,
    val blockedCategoryIds: String,
    val blockedPackages: String,
    val notificationPolicy: String = "ALLOW_ALL",
    val allowedNotificationPackages: String = ""
)
