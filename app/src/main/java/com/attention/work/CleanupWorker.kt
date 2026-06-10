package com.attention.work

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.attention.data.AppDatabase
import java.util.concurrent.TimeUnit

class CleanupWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val database = AppDatabase.getDatabase(applicationContext)
            val articleDao = database.articleDao()
            val summaryDao = database.summaryDao()
            val dailyBriefingDao = database.dailyBriefingDao()

            val now = System.currentTimeMillis()
            val cutoff = now - TimeUnit.DAYS.toMillis(7)

            articleDao.deleteExpiredArticles(now)
            summaryDao.deleteExpiredSummaries(now)
            dailyBriefingDao.deleteOldBriefings(cutoff)

            Result.success()
        } catch (e: Exception) {
            Log.e("CleanupWorker", "Error in daily cleanup worker", e)
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "daily_cleanup"
    }
}
