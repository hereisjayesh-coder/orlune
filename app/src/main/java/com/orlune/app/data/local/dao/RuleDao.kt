package com.orlune.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.orlune.app.data.local.entity.RuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RuleDao {
    @Upsert
    suspend fun upsert(rule: RuleEntity): Long

    @Delete
    suspend fun delete(rule: RuleEntity)

    @Query("SELECT * FROM rules")
    fun observeAll(): Flow<List<RuleEntity>>
}
