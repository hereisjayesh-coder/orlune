package com.orlune.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.orlune.app.data.local.entity.AppListEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppListEntryDao {
    @Upsert
    suspend fun upsert(entry: AppListEntryEntity)

    @Delete
    suspend fun delete(entry: AppListEntryEntity)

    @Query("SELECT * FROM app_list_entries WHERE listType = :listType")
    fun observeByType(listType: String): Flow<List<AppListEntryEntity>>
}
