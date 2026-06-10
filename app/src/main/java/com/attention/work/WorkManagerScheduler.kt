package com.attention.work

import android.content.Context
import android.util.Log
import androidx.work.*
import java.util.Calendar
import java.util.concurrent.TimeUnit


object WorkManagerScheduler {
    
    fun scheduleDailyFetch(context: Context) {
        Log.d("WorkManagerScheduler", "scheduleDailyFetch executing")
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        fun scheduleDailyFetch(context: Context) {
            Log.d("WorkManagerScheduler", "scheduleDailyFetch called")
            // ... rest of function
        }

        // val initialDelay = calculateInitialDelay(7) // Target 7am
        // Temporary test — change back to calculateInitialDelay(7) after confirming
        val initialDelay = 3 * 60 * 1000L // 3 minutes from now

        val fetchRequest = PeriodicWorkRequestBuilder<FetchWorker>(24, TimeUnit.HOURS)
            .setConstraints(constraints)
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .build()

        Log.d("WorkManagerScheduler", "About to enqueue FetchWorker, delay=${initialDelay}ms")

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            FetchWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            fetchRequest
        )

        Log.d("WorkManagerScheduler", "FetchWorker enqueued successfully")
    }

    fun scheduleCleanup(context: Context) {
        val initialDelay = calculateInitialDelay(3) // Target 3am

        val cleanupRequest = PeriodicWorkRequestBuilder<CleanupWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            CleanupWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            cleanupRequest
        )
    }

    private fun calculateInitialDelay(targetHour: Int): Long {
        val currentTime = System.currentTimeMillis()
        val calendar = Calendar.getInstance().apply {
            timeInMillis = currentTime
            set(Calendar.HOUR_OF_DAY, targetHour)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (calendar.timeInMillis <= currentTime) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        return calendar.timeInMillis - currentTime
    }
}
