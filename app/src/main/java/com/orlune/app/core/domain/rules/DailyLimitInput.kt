package com.orlune.app.core.domain.rules

/**
 * Validates and normalizes a daily-limit duration entered as hours+minutes before
 * it's persisted as a RuleEntity.threshold (seconds). The Limits screen's preset
 * chips (30/45/60/90 minutes) are shortcuts into this same function, not a separate
 * unvalidated path — so presets are convenience, not a cap on what's allowed.
 */
object DailyLimitInput {
    const val MIN_TOTAL_MINUTES = 1
    const val MAX_TOTAL_MINUTES = 24 * 60 // a full day — beyond this isn't a "daily" limit

    fun toThresholdSeconds(hours: Int, minutes: Int): Result<Long> {
        if (hours < 0 || minutes < 0) {
            return Result.failure(IllegalArgumentException("Hours and minutes must not be negative"))
        }
        if (minutes >= 60) {
            return Result.failure(IllegalArgumentException("Minutes must be less than 60 — use hours for the rest"))
        }
        val totalMinutes = hours * 60 + minutes
        if (totalMinutes < MIN_TOTAL_MINUTES) {
            return Result.failure(IllegalArgumentException("Duration must be at least $MIN_TOTAL_MINUTES minute"))
        }
        if (totalMinutes > MAX_TOTAL_MINUTES) {
            return Result.failure(IllegalArgumentException("Duration must be at most 24 hours"))
        }
        return Result.success(totalMinutes * 60L)
    }
}
