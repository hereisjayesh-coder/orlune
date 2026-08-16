package com.orlune.app.data.local.entity

import androidx.room.Entity

/** Generic key/value settings not significant enough to warrant their own table. */
@Entity(tableName = "user_preferences", primaryKeys = ["key"])
data class UserPreferenceEntity(
    val key: String,
    val value: String
)
