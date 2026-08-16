package com.orlune.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.orlune.app.data.local.entity.PrivacySettingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PrivacySettingDao {
    @Upsert
    suspend fun upsert(setting: PrivacySettingEntity)

    @Query("SELECT * FROM privacy_settings")
    fun observeAll(): Flow<List<PrivacySettingEntity>>
}
