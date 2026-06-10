package com.attention.data.dao

import androidx.room.*
import com.attention.data.entity.ArticleEntity
import com.attention.data.model.ArticleWithSource
import kotlinx.coroutines.flow.Flow

@Dao
interface ArticleDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertArticles(articles: List<ArticleEntity>): List<Long>

    @Transaction
    @Query("SELECT * FROM articles ORDER BY publishedAt DESC")
    fun getArticlesForFeed(): Flow<List<ArticleWithSource>>

    @Query("SELECT * FROM articles WHERE id = :id")
    suspend fun getArticleById(id: Long): ArticleEntity?

    @Query("SELECT * FROM articles WHERE url = :url")
    suspend fun getArticleByUrl(url: String): ArticleEntity?

    @Query("SELECT * FROM articles WHERE dedupeGroupId = :groupId")
    suspend fun getArticlesByDedupeGroup(groupId: String): List<ArticleEntity>

    @Query("UPDATE articles SET importanceScore = :score WHERE id = :id")
    suspend fun updateImportanceScore(id: Long, score: Int)

    @Query("UPDATE articles SET topicTag = :tag WHERE id = :id")
    suspend fun updateTopicTag(id: Long, tag: String)

    @Query("DELETE FROM articles WHERE expiresAt < :now")
    suspend fun deleteExpiredArticles(now: Long)

    @Query("SELECT * FROM articles WHERE importanceScore IS NULL")
    suspend fun getUnscoredArticles(): List<ArticleEntity>

    @Query("SELECT * FROM articles WHERE fetchedAt >= :startOfDay")
    suspend fun getArticlesForToday(startOfDay: Long): List<ArticleEntity>
}
