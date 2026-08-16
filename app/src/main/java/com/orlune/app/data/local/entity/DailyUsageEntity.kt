package com.orlune.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Aggregated per-app, per-day usage. [epochDay] is days since 1970-01-01
 * (`java.time.LocalDate.toEpochDay()`), the device-local calendar day the usage
 * was recorded on. Raw events are aggregated into this table and not retained
 * indefinitely (Section 8, aggregation-first design).
 */
@Entity(
    tableName = "daily_usage",
    indices = [Index(value = ["packageName", "epochDay"], unique = true)]
)
data class DailyUsageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val epochDay: Long,
    val totalUsageSeconds: Long,
    val launchCount: Int,
    val sessionCount: Int
)
