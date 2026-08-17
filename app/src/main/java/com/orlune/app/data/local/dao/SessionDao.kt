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

    /** One-shot read for the aggregation pipeline (the Flow above is for UI observation). */
    @Query("SELECT * FROM sessions WHERE endTs IS NULL")
    suspend fun getOpenSessions(): List<SessionEntity>

    @Query("SELECT * FROM sessions WHERE packageName = :packageName AND startTs >= :dayStartTs AND startTs < :dayEndTs AND endTs IS NOT NULL")
    suspend fun getClosedSessionsForDay(packageName: String, dayStartTs: Long, dayEndTs: Long): List<SessionEntity>

    /** Total stored session row count, for the Privacy Center's "data stored" summary. */
    @Query("SELECT COUNT(*) FROM sessions")
    fun observeCount(): Flow<Int>

    /** The single longest closed session started within [startTs, endTs) — Insights'
     * "longest usage session" fact. Null when no closed session exists in the window. */
    @Query(
        """
        SELECT * FROM sessions
        WHERE endTs IS NOT NULL AND startTs >= :startTs AND startTs < :endTs
        ORDER BY (endTs - startTs) DESC
        LIMIT 1
        """
    )
    fun observeLongestSessionBetween(startTs: Long, endTs: Long): Flow<SessionEntity?>
}
