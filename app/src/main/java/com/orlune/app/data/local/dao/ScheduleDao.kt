package com.orlune.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.orlune.app.data.local.entity.ScheduleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduleDao {
    @Upsert
    suspend fun upsert(schedule: ScheduleEntity): Long

    @Delete
    suspend fun delete(schedule: ScheduleEntity)

    @Query("SELECT * FROM schedules WHERE associatedRuleId = :ruleId")
    fun observeForRule(ruleId: Long): Flow<List<ScheduleEntity>>
}
