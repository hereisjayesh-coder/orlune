package com.orlune.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.orlune.app.data.local.entity.AppEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    @Upsert
    suspend fun upsert(app: AppEntity)

    @Delete
    suspend fun delete(app: AppEntity)

    @Query("SELECT * FROM apps")
    fun observeAll(): Flow<List<AppEntity>>
}
