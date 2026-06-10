package com.attention.data.repository

import android.util.Log
import com.attention.data.dao.ArticleDao
import com.attention.data.dao.DailyBriefingDao
import com.attention.data.dao.SummaryDao
import com.attention.data.entity.DailyBriefingEntity
import com.attention.data.entity.SummaryEntity
import com.attention.data.remote.ClaudeApiService
import com.attention.data.remote.ClaudeMessage
import com.attention.data.remote.ClaudeRequest
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClaudeRepository @Inject constructor(
    private val claudeApiService: ClaudeApiService,
    private val articleDao: ArticleDao,
    private val dailyBriefingDao: DailyBriefingDao,
    private val summaryDao: SummaryDao
) {
    private val gson = Gson()

    private data class ScoringResult(
        val index: Int,
        val importance: Int,
        val topic: String
    )

    suspend fun scoreAndTagArticles(articleIds: List<Long>? = null) {
        val unscoredArticles = if (articleIds != null) {
            articleDao.getUnscoredArticles().filter { it.id in articleIds }
        } else {
            articleDao.getUnscoredArticles()
        }

        if (unscoredArticles.isEmpty()) return

        val systemPrompt = """
            Return only valid JSON. No preamble. No markdown code fences.
            Analyze the following list of article titles and return a JSON array of objects.
            Each object must contain:
            - "index": the number from the list
            - "importance": integer 1-5 (5 = major industry news, 1 = minor/routine)
            - "topic": one of [models, safety, research, products, policy, other]
        """.trimIndent()

        unscoredArticles.chunked(20).forEach { chunk ->
            try {
                val titlesList = chunk.mapIndexed { index, article ->
                    "${index + 1}. ${article.title}"
                }.joinToString("\n")

                val request = ClaudeRequest(
                    model = "claude-haiku-4-5-20251001",
                    maxTokens = 1024,
                    system = systemPrompt,
                    messages = listOf(
                        ClaudeMessage(role = "user", content = titlesList)
                    )
                )

                val response = claudeApiService.getMessage(request)
                val rawContent = response.content.firstOrNull()?.text ?: return@forEach
                val jsonContent = cleanJson(rawContent)

                val listType = object : TypeToken<List<ScoringResult>>() {}.type
                val results: List<ScoringResult> = gson.fromJson(jsonContent, listType)

                results.forEach { result ->
                    val article = chunk.getOrNull(result.index - 1)
                    if (article != null) {
                        articleDao.updateImportanceScore(article.id, result.importance)
                        articleDao.updateTopicTag(article.id, result.topic)
                    }
                }
            } catch (e: Exception) {
                Log.e("ClaudeRepository", "Error scoring and tagging a chunk of articles", e)
            }
        }
    }

    private data class BriefingResult(
        val briefingText: String,
        val significanceLevel: Int,
        val topStoryTitles: List<String>
    )

    suspend fun generateDailyBriefing() {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val today = dateFormat.format(Date())

        try {
            // 1. Check if briefing already exists for today
            if (dailyBriefingDao.getBriefingForDate(today) != null) return

            // 2. Get today's articles
            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val startOfDay = calendar.timeInMillis
            val todaysArticles = articleDao.getArticlesForToday(startOfDay)

            // Scenario C: Failed - not enough articles
            if (todaysArticles.size < 3) {
                insertFailedBriefing(today, "Today's briefing couldn't be generated — not enough articles were fetched.")
                return
            }

            // 3. Determine Scenario A or B
            val articlesWithScores = todaysArticles.filter { it.importanceScore != null }
            
            val (topArticles, systemPrompt, status) = if (articlesWithScores.isNotEmpty()) {
                // Scenario A: FULL
                val top10 = articlesWithScores
                    .sortedByDescending { it.importanceScore }
                    .take(10)
                
                val articleListString = top10.joinToString("\n") { article ->
                    "- ${article.title} (Importance: ${article.importanceScore})"
                }
                
                val prompt = """
                    Return only valid JSON. No preamble. No markdown code fences.
                    You are a professional news editor. Generate a daily briefing based on the provided list of AI news titles and their importance scores.
                    Return a JSON object with:
                    - "briefingText": 3 sentences maximum, written for a busy person deciding whether to read today's AI news.
                    - "significanceLevel": integer 1-3 (1 = quiet day, 2 = notable, 3 = major news day).
                    - "topStoryTitles": array of the 2-3 most important story titles from the list.
                """.trimIndent()
                
                Triple(articleListString, prompt, "FULL")
            } else {
                // Scenario B: UNRANKED
                val top10 = todaysArticles.take(10)
                val articleListString = top10.joinToString("\n") { "- ${it.title}" }
                
                val prompt = """
                    Return only valid JSON. No preamble. No markdown code fences.
                    You are a professional news editor. Generate a daily briefing based on the provided list of AI news titles.
                    Return a JSON object with:
                    - "briefingText": 3 sentences maximum, written for a busy person deciding whether to read today's AI news.
                    - "significanceLevel": integer 1-3 (1 = quiet day, 2 = notable, 3 = major news day).
                    - "topStoryTitles": array of the 2-3 most important story titles from the list.
                """.trimIndent()
                
                Triple(articleListString, prompt, "UNRANKED")
            }

            val request = ClaudeRequest(
                model = "claude-sonnet-4-6",
                maxTokens = 512,
                system = systemPrompt,
                messages = listOf(
                    ClaudeMessage(role = "user", content = topArticles)
                )
            )

            val response = claudeApiService.getMessage(request)
            val rawContent = response.content.firstOrNull()?.text 
                ?: throw Exception("Empty response from Claude")
            val jsonContent = cleanJson(rawContent)

            val result: BriefingResult = gson.fromJson(jsonContent, BriefingResult::class.java)

            // 5. Insert into Room
            val briefing = DailyBriefingEntity(
                date = today,
                briefingText = result.briefingText,
                significanceLevel = result.significanceLevel,
                topStoryTitles = gson.toJson(result.topStoryTitles),
                generatedAt = System.currentTimeMillis(),
                status = status
            )
            dailyBriefingDao.insertBriefing(briefing)

        } catch (e: Exception) {
            Log.e("ClaudeRepository", "Error generating daily briefing", e)
            // Scenario C: Failed - exception
            insertFailedBriefing(today, "Today's briefing failed due to a technical issue.")
        }
    }

    private suspend fun insertFailedBriefing(date: String, message: String) {
        try {
            if (dailyBriefingDao.getBriefingForDate(date) == null) {
                val failedBriefing = DailyBriefingEntity(
                    date = date,
                    briefingText = message,
                    significanceLevel = 1,
                    topStoryTitles = "[]",
                    generatedAt = System.currentTimeMillis(),
                    status = "FAILED"
                )
                dailyBriefingDao.insertBriefing(failedBriefing)
            }
        } catch (e: Exception) {
            Log.e("ClaudeRepository", "Failed to insert fallback briefing", e)
        }
    }

    private fun cleanJson(json: String): String {
        return json.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
    }

    private data class SummaryResult(
        val summaryText: String,
        val whyItMatters: String
    )

    suspend fun getSummaryForArticle(articleId: Long): SummaryEntity? {
        return try {
            // 1. Check DB first
            val existingSummary = summaryDao.getSummaryForArticle(articleId)
            if (existingSummary != null) return existingSummary

            // 2. Fetch article for content
            val article = articleDao.getArticleById(articleId) ?: return null

            // 3. Call Claude
            val systemPrompt = "Return only valid JSON. No preamble. No markdown code fences."
            val userMessage = """
                Generate a summary for this article.
                Title: ${article.title}
                Excerpt: ${article.excerpt}
                
                Return a JSON object with:
                - "summaryText": 2-3 sentences summarizing the key points.
                - "whyItMatters": one sentence explaining the significance.
            """.trimIndent()

            val request = ClaudeRequest(
                model = "claude-haiku-4-5-20251001",
                maxTokens = 256,
                system = systemPrompt,
                messages = listOf(
                    ClaudeMessage(role = "user", content = userMessage)
                )
            )

            val response = claudeApiService.getMessage(request)
            val rawContent = response.content.firstOrNull()?.text ?: return null
            val jsonContent = cleanJson(rawContent)

            val result: SummaryResult = gson.fromJson(jsonContent, SummaryResult::class.java)

            // 4. Store and return
            val generatedAt = System.currentTimeMillis()
            val summary = SummaryEntity(
                articleId = articleId,
                summaryText = result.summaryText,
                whyItMatters = result.whyItMatters,
                generatedAt = generatedAt,
                expiresAt = generatedAt + (48 * 60 * 60 * 1000)
            )
            summaryDao.insertSummary(summary)
            summary
        } catch (e: Exception) {
            Log.e("ClaudeRepository", "Error getting summary for article $articleId", e)
            null
        }
    }
}
