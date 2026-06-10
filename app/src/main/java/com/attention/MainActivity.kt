package com.attention

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.attention.ui.feed.FeedScreen
import com.attention.ui.sources.SourceManagementScreen
import com.attention.ui.theme.AttentionTheme
import com.attention.util.UrlHelper
import com.attention.work.WorkManagerScheduler
import dagger.hilt.android.AndroidEntryPoint
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.util.Log

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Schedule daily background tasks
        lifecycleScope.launch(Dispatchers.IO) {
            WorkManagerScheduler.scheduleDailyFetch(this@MainActivity)
            WorkManagerScheduler.scheduleCleanup(this@MainActivity)
        }

        // Request notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                0
            )
        }

        Log.d("WorkManagerScheduler", "Calling scheduleDailyFetch from MainActivity")
        WorkManagerScheduler.scheduleDailyFetch(this)

        enableEdgeToEdge()
        setContent {
            AttentionTheme {
                AttentionApp()
            }
        }
    }
}

@Composable
fun AttentionApp() {
    val navController = rememberNavController()
    val context = LocalContext.current
    
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "feed",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("feed") {
                FeedScreen(
                    viewModel = hiltViewModel(),
                    onArticleClick = { url ->
                        UrlHelper.openUrl(context, url)
                    },
                    onSettingsClick = {
                        navController.navigate("sources")
                    }
                )
            }
            composable("sources") {
                SourceManagementScreen(
                    viewModel = hiltViewModel(),
                    onBack = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}
