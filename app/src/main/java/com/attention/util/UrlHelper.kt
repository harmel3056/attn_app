package com.attention.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.ui.graphics.toArgb

object UrlHelper {
    fun openUrl(context: Context, url: String) {
        try {
            val primaryColor = androidx.compose.ui.graphics.Color(0xFF6750A4).toArgb() // Default primary if theme is complex to access here
            val defaultColors = CustomTabColorSchemeParams.Builder()
                .setToolbarColor(primaryColor)
                .build()

            val customTabsIntent = CustomTabsIntent.Builder()
                .setShowTitle(true)
                .setUrlBarHidingEnabled(true)
                .setDefaultColorSchemeParams(defaultColors)
                .build()

            customTabsIntent.launchUrl(context, Uri.parse(url))
        } catch (e: Exception) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        }
    }
}
