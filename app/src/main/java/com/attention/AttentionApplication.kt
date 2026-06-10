package com.attention

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.work.WorkManager
import com.attention.util.NotificationHelper
import dagger.hilt.android.HiltAndroidApp
import androidx.work.Configuration

@HiltAndroidApp
class AttentionApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        Log.d("AttentionApp", "Application onCreate started")
        if (!WorkManager.isInitialized()) {
            WorkManager.initialize(
                this,
                Configuration.Builder()
                    .setMinimumLoggingLevel(android.util.Log.DEBUG)
                    .build()
            )
        }

        Log.d("AttentionApp", "WorkManager initialized")
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Daily Briefing"
            val descriptionText = "Your morning AI news briefing"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(NotificationHelper.CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
