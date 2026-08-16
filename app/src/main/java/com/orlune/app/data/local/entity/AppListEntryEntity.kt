package com.orlune.app.data.local.entity

import androidx.room.Entity

/**
 * Explicit block/allow list entry, including essential-app exemptions.
 * [listType] is "block" or "allow" — Section 8's separate BlockRule/AllowRule
 * rows, merged into one table since they share an identical shape.
 */
@Entity(tableName = "app_list_entries", primaryKeys = ["packageName", "listType"])
data class AppListEntryEntity(
    val packageName: String,
    val listType: String
)
