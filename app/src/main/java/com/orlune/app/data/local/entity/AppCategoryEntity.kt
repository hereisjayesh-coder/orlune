package com.orlune.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** User- or system-defined grouping that apps can belong to. */
@Entity(tableName = "app_categories")
data class AppCategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val isSystemDefined: Boolean
)
