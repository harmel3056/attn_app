package com.attention.data.dao

import androidx.room.*
import com.attention.data.entity.DailyBriefingEntity

@Dao
interface DailyBriefingDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBriefing(briefing: DailyBriefingEntity)

    @Query("SELECT * FROM daily_briefings WHERE date = :date")
    suspend fun getBriefingForDate(date: String): DailyBriefingEntity?

    @Query("SELECT * FROM daily_briefings ORDER BY generatedAt DESC LIMIT 1")
    fun getLatestBriefing(): kotlinx.coroutines.flow.Flow<DailyBriefingEntity?>

    @Query("SELECT * FROM daily_briefings ORDER BY generatedAt DESC LIMIT 1")
    suspend fun getLatestBriefingOnce(): DailyBriefingEntity?

    @Query("DELETE FROM daily_briefings WHERE generatedAt < :cutoff")
    suspend fun deleteOldBriefings(cutoff: Long)

    @Query("DELETE FROM daily_briefings")
    suspend fun clearAll()
}
