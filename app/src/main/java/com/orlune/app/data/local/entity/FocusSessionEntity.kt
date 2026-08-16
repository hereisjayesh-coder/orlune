package com.orlune.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A user-initiated focus period. [blockedCategoryIds] is a comma-separated list of
 * [AppCategoryEntity] ids; parsing/serialization is the rule engine's concern
 * (Phase 4+), not the persistence layer's.
 */
@Entity(tableName = "focus_sessions")
data class FocusSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startTs: Long,
    val endTs: Long?,
    val plannedMinutes: Int,
    val completedMinutes: Int,
    val blockedCategoryIds: String
)
