package com.orlune.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Single-row settings table (always `id = SINGLETON_ID`). [quietWindows]'s
 * serialization format is owned by the feature that reads it (Phase 6+), not
 * the persistence layer.
 */
@Entity(tableName = "notification_preferences")
data class NotificationPreferenceEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val quietWindows: String,
    val digestEnabled: Boolean
) {
    companion object {
        const val SINGLETON_ID = 0
    }
}
