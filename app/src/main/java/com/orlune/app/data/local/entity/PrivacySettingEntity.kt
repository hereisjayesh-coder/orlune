package com.orlune.app.data.local.entity

import androidx.room.Entity

/** Per-permission status snapshot shown in the Privacy Center (Phase 9). */
@Entity(tableName = "privacy_settings", primaryKeys = ["permissionName"])
data class PrivacySettingEntity(
    val permissionName: String,
    val granted: Boolean,
    val lastChecked: Long
)
