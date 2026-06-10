package com.attention.data.dao

import androidx.room.*
import com.attention.data.entity.SourceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SourceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSource(source: SourceEntity): Long

    @Query("SELECT * FROM sources")
    fun getAllSources(): Flow<List<SourceEntity>>

    @Query("SELECT * FROM sources WHERE isEnabled = 1")
    fun getEnabledSources(): Flow<List<SourceEntity>>

    @Query("UPDATE sources SET lastFetched = :timestamp WHERE id = :id")
    suspend fun updateLastFetched(id: Long, timestamp: Long)

    @Query("DELETE FROM sources WHERE id = :id")
    suspend fun deleteSource(id: Long)

    @Query("UPDATE sources SET isEnabled = :isEnabled WHERE id = :id")
    suspend fun updateEnabled(id: Long, isEnabled: Boolean)
}
