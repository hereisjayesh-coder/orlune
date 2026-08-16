package com.orlune.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.orlune.app.data.local.entity.LocalRecommendationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LocalRecommendationDao {
    @Upsert
    suspend fun upsert(recommendation: LocalRecommendationEntity): Long

    @Delete
    suspend fun delete(recommendation: LocalRecommendationEntity)

    @Query("SELECT * FROM local_recommendations ORDER BY epochDay DESC")
    fun observeAll(): Flow<List<LocalRecommendationEntity>>
}
