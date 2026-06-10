package com.attention.data.repository

import android.util.Log
import com.attention.data.dao.ArticleDao
import com.attention.data.dao.SourceDao
import com.attention.data.remote.FeedFetcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class NewsRepository(
    private val articleDao: ArticleDao,
    private val sourceDao: SourceDao,
    private val feedFetcher: FeedFetcher,
    private val claudeRepository: ClaudeRepository
) {

    /**
     * Fetches new articles from all enabled sources concurrently and stores them in the database.
     * Returns a flattened list of all newly inserted article IDs.
     */
    suspend fun fetchAndStoreArticles(): List<Long> = withContext(Dispatchers.IO) {
        // Get the current list of enabled sources (one-shot)
        val enabledSources = sourceDao.getEnabledSources().first()

        // Use coroutineScope to manage concurrent fetching
        val results = coroutineScope {
            enabledSources.map { source ->
                async {
                    try {
                        // 1. Fetch articles from the remote feed
                        val fetchedArticles = feedFetcher.fetchFeed(source)
                        
                        // 2. Filter out articles that already exist in the DB by URL
                        val newArticles = fetchedArticles.filter { article ->
                            articleDao.getArticleByUrl(article.url) == null
                        }

                        // 3. Insert only new articles
                        if (newArticles.isNotEmpty()) {
                            articleDao.insertArticles(newArticles)
                        } else {
                            emptyList()
                        }
                    } catch (e: Exception) {
                        Log.e("NewsRepository", "Error fetching from ${source.feedUrl}", e)
                        emptyList()
                    } finally {
                        // 4. Update the last fetched timestamp for this source
                        sourceDao.updateLastFetched(source.id, System.currentTimeMillis())
                    }
                }
            }.awaitAll()
        }
        results.flatten().filter { it != -1L }
    }

    /**
     * Legacy method for backward compatibility if needed, 
     * but now we prefer calling the individual steps for progress reporting.
     */
    suspend fun fetchAndStoreNewArticles() {
        val allInsertedIds = fetchAndStoreArticles()
        if (allInsertedIds.isNotEmpty()) {
            claudeRepository.scoreAndTagArticles(allInsertedIds)
        }
        claudeRepository.generateDailyBriefing()
    }
}
