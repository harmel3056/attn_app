package com.attention.data.remote

import android.util.Log
import com.attention.data.entity.ArticleEntity
import com.attention.data.entity.SourceEntity
import com.rometools.rome.feed.synd.SyndEntry
import com.rometools.rome.feed.synd.SyndFeed
import com.rometools.rome.io.SyndFeedInput
import com.rometools.rome.io.XmlReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Url
import java.util.concurrent.TimeUnit

interface FeedService {
    @GET
    suspend fun getRawFeed(@Url url: String): ResponseBody
}

/**
 * FeedFetcher handles retrieving and parsing RSS/Atom feeds using Retrofit and Rome.
 */
class FeedFetcher {
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .callTimeout(20, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://localhost/")
        .client(okHttpClient)
        .build()

    private val service = retrofit.create(FeedService::class.java)

    /**
     * Fetches and parses a feed from the provided [source].
     * 
     * Rome parsing model explanation:
     * - [SyndFeed]: An object-oriented representation of the entire feed (RSS or Atom).
     *   It abstracts away the differences between formats, providing a common interface 
     *   for feed metadata like title, description, and the list of entries.
     * - [SyndEntry]: Represents a single "item" in an RSS feed or an "entry" in an Atom feed.
     *   It contains the article-specific data like title, link, and publication date.
     * 
     * @return A list of [ArticleEntity] mapped from the feed entries.
     */
    suspend fun fetchFeed(source: SourceEntity): List<ArticleEntity> {
        Log.d("FeedFetcher", "Starting fetch for ${source.name}: ${source.feedUrl}")
        return try {
            withContext(Dispatchers.IO) {
                val startTime = System.currentTimeMillis()
                val responseBody = service.getRawFeed(source.feedUrl)
                val inputStream = responseBody.byteStream()

                // SyndFeedInput handles format detection (RSS 0.9x, 1.0, 2.0, Atom 0.3, 1.0)
                val input = SyndFeedInput()
                val feed: SyndFeed = XmlReader(inputStream).use { reader ->
                    input.build(reader)
                }

                val fetchedAt = System.currentTimeMillis()
                val expiresAt = fetchedAt + (48 * 60 * 60 * 1000)
                val cutoff = fetchedAt - (48 * 60 * 60 * 1000)

                val articles = feed.entries.mapNotNull { entry: SyndEntry ->
                    // Defensively handle missing URL: skip entry
                    val url = entry.link ?: return@mapNotNull null

                    ArticleEntity(
                        sourceId = source.id,
                        title = entry.title ?: "", // Empty string for missing title
                        url = url,
                        // Current time if published date is missing
                        publishedAt = entry.publishedDate?.time ?: fetchedAt,
                        fetchedAt = fetchedAt,
                        // Use description as excerpt, or empty string
                        excerpt = entry.description?.value ?: "",
                        importanceScore = null,
                        topicTag = "other",
                        expiresAt = expiresAt
                    )
                }
                    .filter { it.publishedAt >= cutoff }
                    .take(10)

                Log.d("FeedFetcher", "Fetched ${source.name} in ${System.currentTimeMillis() - startTime}ms — ${articles.size} articles")
                articles
            }
        } catch (e: Exception) {
            Log.e("FeedFetcher", "Error fetching/parsing feed: ${source.feedUrl}", e)
            emptyList()
        }
    }
}
