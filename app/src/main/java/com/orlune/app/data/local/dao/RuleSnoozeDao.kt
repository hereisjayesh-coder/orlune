package com.orlune.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.orlune.app.data.local.entity.RuleSnoozeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RuleSnoozeDao {
    @Upsert
    suspend fun upsert(snooze: RuleSnoozeEntity)

    @Query("SELECT * FROM rule_snoozes WHERE packageName = :packageName LIMIT 1")
    suspend fun get(packageName: String): RuleSnoozeEntity?

    @Query("DELETE FROM rule_snoozes WHERE packageName = :packageName")
    suspend fun delete(packageName: String)

    @Query("SELECT * FROM rule_snoozes")
    fun observeAll(): Flow<List<RuleSnoozeEntity>>
}
