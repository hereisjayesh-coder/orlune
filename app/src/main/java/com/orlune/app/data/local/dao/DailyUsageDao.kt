package com.orlune.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.orlune.app.data.local.entity.DailyUsageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyUsageDao {
    @Upsert
    suspend fun upsert(usage: DailyUsageEntity): Long

    @Delete
    suspend fun delete(usage: DailyUsageEntity)

    @Query("SELECT * FROM daily_usage WHERE packageName = :packageName ORDER BY epochDay DESC")
    fun observeForApp(packageName: String): Flow<List<DailyUsageEntity>>

    @Query("SELECT * FROM daily_usage WHERE epochDay = :epochDay")
    fun observeForDay(epochDay: Long): Flow<List<DailyUsageEntity>>
}
