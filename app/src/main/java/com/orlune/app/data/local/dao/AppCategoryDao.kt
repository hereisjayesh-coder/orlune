package com.orlune.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.orlune.app.data.local.entity.AppCategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppCategoryDao {
    @Upsert
    suspend fun upsert(category: AppCategoryEntity): Long

    @Delete
    suspend fun delete(category: AppCategoryEntity)

    @Query("SELECT * FROM app_categories")
    fun observeAll(): Flow<List<AppCategoryEntity>>
}
