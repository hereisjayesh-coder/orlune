package com.orlune.app.core.domain.rules

/**
 * Goal progress, per `docs/phase-0-research.md` Section 9 (GoalEngine:
 * "completedUnits / plannedUnits"). Units are whatever the caller's goal type means
 * (minutes for a focus goal, seconds for a usage goal, etc.) — this engine is unit-agnostic.
 */
object GoalEngine {

    /**
     * [plannedUnits] <= 0 returns 0.0 (guards divide-by-zero rather than throwing,
     * consistent with UsageAggregator's empty-input-returns-zero style) — a goal with
     * no planned target has no meaningful progress. Not clamped to 1.0: a caller that
     * over-completes a goal gets a ratio above 1.0, and decides how to display that.
     */
    fun progress(completedUnits: Long, plannedUnits: Long): Double =
        if (plannedUnits <= 0) 0.0 else completedUnits.toDouble() / plannedUnits
}
