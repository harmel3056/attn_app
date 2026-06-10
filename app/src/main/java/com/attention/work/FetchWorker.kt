package com.attention.work

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.attention.data.AppDatabase
import com.attention.data.remote.ClaudeRetrofitClient
import com.attention.data.remote.FeedFetcher
import com.attention.data.repository.ClaudeRepository
import com.attention.data.repository.NewsRepository
import com.attention.util.NotificationHelper

class FetchWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Log.d("FetchWorker", "doWork() started")

        return try {
            val database = AppDatabase.getDatabase(applicationContext)
            val articleDao = database.articleDao()
            val sourceDao = database.sourceDao()
            val summaryDao = database.summaryDao()
            val dailyBriefingDao = database.dailyBriefingDao()
            
            val feedFetcher = FeedFetcher()
            val claudeApiService = ClaudeRetrofitClient.create()
            
            val claudeRepository = ClaudeRepository(claudeApiService, articleDao, dailyBriefingDao, summaryDao)
            val newsRepository = NewsRepository(articleDao, sourceDao, feedFetcher, claudeRepository)

            val insertedIds = newsRepository.fetchAndStoreArticles()
            if (insertedIds.isNotEmpty()) {
                claudeRepository.scoreAndTagArticles(insertedIds)
            }
            claudeRepository.generateDailyBriefing()
            
            val latestBriefing = dailyBriefingDao.getLatestBriefingOnce()
            if (latestBriefing != null && latestBriefing.status != "FAILED") {
                NotificationHelper.sendBriefingNotification(applicationContext, latestBriefing)
            }

            Result.success()
        } catch (e: Exception) {
            Log.e("FetchWorker", "Error in daily fetch worker", e)
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "daily_fetch"
    }
}
