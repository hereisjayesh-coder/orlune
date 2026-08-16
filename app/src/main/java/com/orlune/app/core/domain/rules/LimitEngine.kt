package com.orlune.app.core.domain.rules

enum class LimitState { UNDER_LIMIT, AT_OR_OVER_LIMIT }

/**
 * Evaluates a single usage limit, per `docs/phase-0-research.md` Section 9
 * (LimitEngine: "currentUsage vs. configured threshold"). Binary rather than the
 * illustrative 3-tier "warn/friction/block" output — `RuleEntity` only has one
 * `threshold` field, and MVP's blocking levels (Reminder fires on crossing, Block
 * enforces after) don't need a third state; a warning tier would need a config
 * field that doesn't exist yet.
 */
object LimitEngine {

    /** [thresholdSeconds] <= 0 is treated as always exceeded — there's no usage under a zero limit. */
    fun evaluate(currentUsageSeconds: Long, thresholdSeconds: Long): LimitState =
        if (thresholdSeconds <= 0 || currentUsageSeconds >= thresholdSeconds) {
            LimitState.AT_OR_OVER_LIMIT
        } else {
            LimitState.UNDER_LIMIT
        }
}
