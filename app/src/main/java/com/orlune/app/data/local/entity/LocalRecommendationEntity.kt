package com.orlune.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Output of the deterministic RecommendationEngine (Phase 7, deferred past MVP —
 * see `docs/phase-0-research.md` Section 13). The table exists ahead of the
 * feature per the Phase 2 schema-first plan (Section 14).
 */
@Entity(tableName = "local_recommendations")
data class LocalRecommendationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val epochDay: Long,
    val ruleSuggestion: String,
    val basis: String
)
