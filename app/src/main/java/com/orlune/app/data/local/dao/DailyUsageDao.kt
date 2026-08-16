package com.orlune.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.orlune.app.data.local.entity.DailyUsageEntity
import kotlinx.coroutines.flow.Flow

/** Query projection joining daily_usage with apps, for display purposes only. */
data class AppDailyUsage(
    val packageName: String,
    val label: String?,
    val totalUsageSeconds: Long,
    val sessionCount: Int
)

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

    /**
     * Room's @Upsert matches on primary key, not the (packageName, epochDay) unique
     * index — so callers must look up the existing row (if any) themselves and reuse
     * its id, rather than relying on @Upsert to detect the natural-key conflict.
     */
    @Query("SELECT * FROM daily_usage WHERE packageName = :packageName AND epochDay = :epochDay LIMIT 1")
    suspend fun getForAppAndDay(packageName: String, epochDay: Long): DailyUsageEntity?

    @Query(
        """
        SELECT daily_usage.packageName AS packageName, apps.label AS label,
               daily_usage.totalUsageSeconds AS totalUsageSeconds, daily_usage.sessionCount AS sessionCount
        FROM daily_usage
        LEFT JOIN apps ON apps.packageName = daily_usage.packageName
        WHERE daily_usage.epochDay = :epochDay
        ORDER BY daily_usage.totalUsageSeconds DESC
        """
    )
    fun observeForDayWithLabels(epochDay: Long): Flow<List<AppDailyUsage>>
}
