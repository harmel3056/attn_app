package com.attention.ui.feed

import android.text.format.DateUtils
import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.attention.R
import com.attention.data.entity.DailyBriefingEntity
import com.attention.data.model.ArticleWithSource
import com.attention.ui.feed.FilterBottomSheet
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FeedScreen(
    viewModel: FeedViewModel,
    onArticleClick: (String) -> Unit,
    onSettingsClick: () -> Unit
) {
    val articles by viewModel.articles.collectAsState()
    val latestBriefing by viewModel.latestBriefing.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val currentFilter by viewModel.currentFilter.collectAsState()
    val summaries by viewModel.summaries.collectAsState()
    val refreshStatus by viewModel.refreshStatus.collectAsState()
    val todayCount by viewModel.todayArticleCount.collectAsState()
    
    var showFilterSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val dateString = remember {
                        SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(Date())
                    }
                    Text(
                        text = dateString,
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                actions = {
                    IconButton(onClick = { showFilterSheet = true }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filter")
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (showFilterSheet) {
            FilterBottomSheet(
                currentFilter = currentFilter,
                onFilterChange = { viewModel.updateFilter(it) },
                onDismiss = { showFilterSheet = false }
            )
        }

        Column(modifier = Modifier.padding(paddingValues)) {
            AnimatedVisibility(
                visible = refreshStatus != RefreshStatus.Idle,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                val statusText = when (refreshStatus) {
                    RefreshStatus.FetchingSources -> "Fetching sources..."
                    RefreshStatus.ScoringArticles -> "Scoring articles with AI..."
                    RefreshStatus.GeneratingBriefing -> "Generating your briefing..."
                    RefreshStatus.Complete -> "Complete"
                    else -> ""
                }
                
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    textAlign = TextAlign.Center
                )
            }

            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = { viewModel.refresh() },
                modifier = Modifier.fillMaxSize()
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        DailyBriefingCard(
                            briefing = latestBriefing,
                            onTopStoryClick = { title ->
                                articles.find { it.article.title == title }?.let {
                                    onArticleClick(it.article.url)
                                }
                            }
                        )
                    }

                    if (todayCount > 0) {
                        item {
                            Text(
                                text = "— $todayCount articles today —",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    if (articles.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillParentMaxSize()
                                    .padding(bottom = 100.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No articles yet — your first fetch runs at 7am",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        items(articles, key = { it.article.id }) { articleWithSource ->
                            ArticleCard(
                                articleWithSource = articleWithSource,
                                summaryState = summaries[articleWithSource.article.id] ?: SummaryState.Idle,
                                onArticleClick = onArticleClick,
                                onToggleSummary = { viewModel.toggleSummary(articleWithSource.article.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DailyBriefingCard(
    briefing: DailyBriefingEntity?,
    onTopStoryClick: (String) -> Unit
) {
    val cardBackground = Color(0xFFFFF0F5)
    val contentColor = Color(0xFF4A2030)

    if (briefing == null) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = cardBackground)
        ) {
            Text(
                text = "Fetching your first briefing...",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor
            )
        }
    } else {
        val isFailed = briefing.status == "FAILED"
        val isUnranked = briefing.status == "UNRANKED"

        val significanceColor = when (briefing.significanceLevel) {
            1 -> Color(0xFFE8A0B4)
            2 -> Color(0xFFE85D8A)
            3 -> Color(0xFFD63A6B)
            else -> Color(0xFFE8A0B4)
        }

        val significanceLabel = when (briefing.significanceLevel) {
            1 -> "Quiet day"
            2 -> "Notable"
            3 -> "Major news day"
            else -> ""
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (isFailed) MaterialTheme.colorScheme.errorContainer
                else cardBackground
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.brain_icon),
                        contentDescription = "Significance",
                        modifier = Modifier.size(80.dp),
                        colorFilter = ColorFilter.tint(significanceColor, BlendMode.SrcIn)
                    )

                    Spacer(Modifier.width(12.dp))

                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Today in AI",
                            style = MaterialTheme.typography.titleLarge,
                            color = if (isFailed) MaterialTheme.colorScheme.onErrorContainer else contentColor
                        )
                        Text(
                            text = significanceLabel,
                            style = MaterialTheme.typography.labelLarge,
                            color = significanceColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = briefing.briefingText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isFailed) MaterialTheme.colorScheme.onErrorContainer
                    else contentColor
                )

                if (!isFailed) {
                    val gson = remember { Gson() }
                    val titles: List<String> = remember(briefing.topStoryTitles) {
                        val type = object : TypeToken<List<String>>() {}.type
                        gson.fromJson(briefing.topStoryTitles, type) ?: emptyList()
                    }

                    Column(modifier = Modifier.padding(top = 8.dp)) {
                        titles.forEach { title ->
                            Row(modifier = Modifier.padding(vertical = 2.dp)) {
                                Text("• ", style = MaterialTheme.typography.bodySmall, color = significanceColor)
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFF272827),
                                    textDecoration = TextDecoration.Underline,
                                    modifier = Modifier.clickable { onTopStoryClick(title) }
                                )
                            }
                        }
                    }
                }

                if (isUnranked) {
                    Text(
                        text = "Note: article ranking unavailable today",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(top = 8.dp),
                        color = (if (isFailed) MaterialTheme.colorScheme.onErrorContainer else contentColor).copy(
                            alpha = 0.7f
                        )
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ArticleCard(
    articleWithSource: ArticleWithSource,
    summaryState: SummaryState,
    onArticleClick: (String) -> Unit,
    onToggleSummary: () -> Unit
) {
    val article = articleWithSource.article
    val source = articleWithSource.source

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onArticleClick(article.url) }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = source.name.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(text = article.title, style = MaterialTheme.typography.titleSmall)
            
            Spacer(modifier = Modifier.height(8.dp))
            
            val relativeTime = DateUtils.getRelativeTimeSpanString(
                article.publishedAt,
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS
            )
            
            Text(
                text = relativeTime.toString(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AssistChip(
                        onClick = { },
                        label = { Text(article.topicTag) }
                    )
                    
                    article.importanceScore?.let { score ->
                        Spacer(modifier = Modifier.width(12.dp))
                        ImportanceIndicator(score)
                    }
                }

                if (summaryState is SummaryState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    val rotation by animateFloatAsState(
                        targetValue = if (summaryState is SummaryState.Success) 180f else 0f,
                        label = "chevronRotation"
                    )
                    IconButton(onClick = onToggleSummary) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = if (summaryState is SummaryState.Success) "Hide Summary" else "Show Summary",
                            modifier = Modifier.rotate(rotation)
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = summaryState is SummaryState.Success,
                enter = fadeIn() + expandVertically(),
            ) {
                if (summaryState is SummaryState.Success) {
                    Column(modifier = Modifier.padding(top = 16.dp)) {
                        HorizontalDivider(modifier = Modifier.padding(bottom = 12.dp))
                        Text(
                            text = summaryState.summary.summaryText,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Why it matters: ${summaryState.summary.whyItMatters}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            if (summaryState is SummaryState.Error) {
                Text(
                    text = "Failed to load summary",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

@Composable
fun ImportanceIndicator(score: Int) {
    Row {
        repeat(5) { index ->
            val isFilled = index < score
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .padding(1.dp)
                    .background(
                        color = if (isFilled) MaterialTheme.colorScheme.primary 
                                else MaterialTheme.colorScheme.outlineVariant,
                        shape = androidx.compose.foundation.shape.CircleShape
                    )
            )
        }
    }
}
