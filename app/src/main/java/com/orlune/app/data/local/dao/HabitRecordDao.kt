package com.orlune.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.orlune.app.data.local.entity.HabitRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitRecordDao {
    @Upsert
    suspend fun upsert(record: HabitRecordEntity): Long

    @Delete
    suspend fun delete(record: HabitRecordEntity)

    @Query("SELECT * FROM habit_records WHERE goalId = :goalId ORDER BY epochDay DESC")
    fun observeForGoal(goalId: Long): Flow<List<HabitRecordEntity>>
}
