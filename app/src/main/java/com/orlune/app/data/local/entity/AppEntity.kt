package com.orlune.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Known app metadata, one row per installed app Orlune has seen. */
@Entity(tableName = "apps")
data class AppEntity(
    @PrimaryKey val packageName: String,
    val label: String,
    val category: String?,
    val isEssential: Boolean
)
