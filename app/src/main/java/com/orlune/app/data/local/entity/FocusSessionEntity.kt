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
 * No stored status column: a session's state ([com.orlune.app.core.domain.focus.FocusSessionEngine.stateOf])
 * is a pure function of [startTs]/[endTs]/[plannedMinutes]/[completedMinutes] and the
 * current time, so a crash/reboot/restart can never leave a session in a state that
 * doesn't reconcile correctly once something reads it again.
 */
@Entity(tableName = "focus_sessions")
data class FocusSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startTs: Long,
    val endTs: Long?,
    val plannedMinutes: Int,
    val completedMinutes: Int,
    val blockedCategoryIds: String,
    val blockedPackages: String
)
