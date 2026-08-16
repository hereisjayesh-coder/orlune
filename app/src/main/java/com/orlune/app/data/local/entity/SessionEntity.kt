package com.orlune.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A single app-open-to-close session. Short retention by design — rolls into
 * [DailyUsageEntity] and is not kept indefinitely (Section 8, aggregation-first).
 */
@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val startTs: Long,
    val endTs: Long?
)
