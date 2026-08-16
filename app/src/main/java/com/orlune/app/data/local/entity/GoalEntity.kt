package com.orlune.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A user-defined target. [type] is "usage", "focus", or "balance". */
@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val targetValue: Long,
    val period: String
)
