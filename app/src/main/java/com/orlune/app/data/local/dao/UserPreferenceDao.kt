package com.orlune.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.orlune.app.data.local.entity.UserPreferenceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserPreferenceDao {
    @Upsert
    suspend fun upsert(preference: UserPreferenceEntity)

    @Delete
    suspend fun delete(preference: UserPreferenceEntity)

    @Query("SELECT * FROM user_preferences WHERE `key` = :key")
    fun observe(key: String): Flow<UserPreferenceEntity?>
}
