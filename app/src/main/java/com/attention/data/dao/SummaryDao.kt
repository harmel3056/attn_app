package com.attention.data.dao

import androidx.room.*
import com.attention.data.entity.SummaryEntity

@Dao
interface SummaryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSummary(summary: SummaryEntity)

    @Query("SELECT * FROM summaries WHERE articleId = :articleId")
    suspend fun getSummaryForArticle(articleId: Long): SummaryEntity?

    @Query("DELETE FROM summaries WHERE expiresAt < :now")
    suspend fun deleteExpiredSummaries(now: Long)
}
