package com.orlune.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.orlune.app.data.local.entity.FocusSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FocusSessionDao {
    @Upsert
    suspend fun upsert(session: FocusSessionEntity): Long

    @Delete
    suspend fun delete(session: FocusSessionEntity)

    @Query("SELECT * FROM focus_sessions ORDER BY startTs DESC")
    fun observeAll(): Flow<List<FocusSessionEntity>>
}
