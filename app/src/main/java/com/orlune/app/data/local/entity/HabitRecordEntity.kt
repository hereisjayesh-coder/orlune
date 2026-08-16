package com.orlune.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Rolling consistency tracking: whether [goalId] was met on [epochDay]
 * (days since 1970-01-01, `java.time.LocalDate.toEpochDay()`).
 */
@Entity(
    tableName = "habit_records",
    foreignKeys = [
        ForeignKey(
            entity = GoalEntity::class,
            parentColumns = ["id"],
            childColumns = ["goalId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["goalId", "epochDay"], unique = true)]
)
data class HabitRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val epochDay: Long,
    val goalId: Long,
    val met: Boolean
)
