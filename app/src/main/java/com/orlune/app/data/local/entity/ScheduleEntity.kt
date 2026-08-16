package com.orlune.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A recurring time window associated with a rule. [daysOfWeek] is a comma-separated
 * list (e.g. "MON,TUE,WED"); [startTime]/[endTime] are "HH:mm" device-local times.
 */
@Entity(
    tableName = "schedules",
    foreignKeys = [
        ForeignKey(
            entity = RuleEntity::class,
            parentColumns = ["id"],
            childColumns = ["associatedRuleId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("associatedRuleId")]
)
data class ScheduleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val daysOfWeek: String,
    val startTime: String,
    val endTime: String,
    val associatedRuleId: Long
)
