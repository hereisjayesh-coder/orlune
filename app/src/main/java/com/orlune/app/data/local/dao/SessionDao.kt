package com.orlune.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.orlune.app.data.local.entity.SessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Upsert
    suspend fun upsert(session: SessionEntity): Long

    @Delete
    suspend fun delete(session: SessionEntity)

    @Query("SELECT * FROM sessions WHERE packageName = :packageName ORDER BY startTs DESC")
    fun observeForApp(packageName: String): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE endTs IS NULL")
    fun observeOpenSessions(): Flow<List<SessionEntity>>
}
