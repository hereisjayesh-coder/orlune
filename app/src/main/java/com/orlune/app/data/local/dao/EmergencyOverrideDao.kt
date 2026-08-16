package com.orlune.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.orlune.app.data.local.entity.EmergencyOverrideEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EmergencyOverrideDao {
    @Upsert
    suspend fun upsert(override: EmergencyOverrideEntity): Long

    @Delete
    suspend fun delete(override: EmergencyOverrideEntity)

    @Query("SELECT * FROM emergency_overrides ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<EmergencyOverrideEntity>>
}
