package com.orlune.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.orlune.app.data.local.entity.GoalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalDao {
    @Upsert
    suspend fun upsert(goal: GoalEntity): Long

    @Delete
    suspend fun delete(goal: GoalEntity)

    @Query("SELECT * FROM goals")
    fun observeAll(): Flow<List<GoalEntity>>
}
