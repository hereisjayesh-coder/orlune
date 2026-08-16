package com.orlune.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.orlune.app.data.local.entity.ThemePreferenceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ThemePreferenceDao {
    @Upsert
    suspend fun upsert(preference: ThemePreferenceEntity)

    @Query("SELECT * FROM theme_preferences WHERE id = :id")
    fun observe(id: Int = ThemePreferenceEntity.SINGLETON_ID): Flow<ThemePreferenceEntity?>
}
