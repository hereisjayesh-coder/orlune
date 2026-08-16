package com.orlune.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A single enforceable rule. [type] is one of "limit", "schedule", "block" — kept as
 * a plain string in Phase 2 (schema only); the deterministic rule engine (Phase 4)
 * owns the real type and its evaluation semantics.
 */
@Entity(tableName = "rules")
data class RuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val targetPackageOrCategory: String,
    val threshold: Long?,
    val windowDefinition: String?
)
