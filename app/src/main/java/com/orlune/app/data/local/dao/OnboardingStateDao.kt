package com.orlune.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.orlune.app.data.local.entity.OnboardingStateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OnboardingStateDao {
    @Upsert
    suspend fun upsert(state: OnboardingStateEntity)

    @Query("SELECT * FROM onboarding_state WHERE id = :id")
    fun observe(id: Int = OnboardingStateEntity.SINGLETON_ID): Flow<OnboardingStateEntity?>
}
