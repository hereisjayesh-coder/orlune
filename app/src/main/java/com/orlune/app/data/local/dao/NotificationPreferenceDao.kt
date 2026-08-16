package com.orlune.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.orlune.app.data.local.entity.NotificationPreferenceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationPreferenceDao {
    @Upsert
    suspend fun upsert(preference: NotificationPreferenceEntity)

    @Query("SELECT * FROM notification_preferences WHERE id = :id")
    fun observe(id: Int = NotificationPreferenceEntity.SINGLETON_ID): Flow<NotificationPreferenceEntity?>
}
