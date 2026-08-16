package com.orlune.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Single-row settings table (always `id = SINGLETON_ID`). [themeId] is "light"/"dark"/"forest". */
@Entity(tableName = "theme_preferences")
data class ThemePreferenceEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val themeId: String
) {
    companion object {
        const val SINGLETON_ID = 0
    }
}
