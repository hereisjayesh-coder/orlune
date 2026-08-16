package com.orlune.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Logged override usage, kept for the user's own transparency (Privacy Center,
 * Phase 9) — not used for enforcement.
 */
@Entity(tableName = "emergency_overrides")
data class EmergencyOverrideEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val ruleId: Long,
    val reason: String?
)
