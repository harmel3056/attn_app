package com.attention.ui.feed

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.attention.data.dao.ArticleDao
import com.attention.data.dao.DailyBriefingDao
import com.attention.data.entity.DailyBriefingEntity
import com.attention.data.entity.SummaryEntity
import com.attention.data.model.ArticleWithSource
import com.attention.data.repository.ClaudeRepository
import com.attention.data.repository.NewsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class SortOrder {
    object Newest : SortOrder()
    object Oldest : SortOrder()
    object MostCovered : SortOrder()
}

sealed class SummaryState {
    object Idle : SummaryState()
    object Loading : SummaryState()
    data class Success(val summary: SummaryEntity) : SummaryState()
    object Error : SummaryState()
}

sealed class RefreshStatus {
    object Idle : RefreshStatus()
    object FetchingSources : RefreshStatus()
    object ScoringArticles : RefreshStatus()
    object GeneratingBriefing : RefreshStatus()
    object Complete : RefreshStatus()
}

data class FeedFilter(
    val topicTag: String? = null,
    val sortOrder: SortOrder = SortOrder.Newest
)

@HiltViewModel
class FeedViewModel @Inject constructor(
    private val articleDao: ArticleDao,
    private val dailyBriefingDao: DailyBriefingDao,
    private val newsRepository: NewsRepository,
    private val claudeRepository: ClaudeRepository
) : ViewModel() {

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _refreshStatus = MutableStateFlow<RefreshStatus>(RefreshStatus.Idle)
    val refreshStatus: StateFlow<RefreshStatus> = _refreshStatus.asStateFlow()

    private val _currentFilter = MutableStateFlow(FeedFilter())
    val currentFilter: StateFlow<FeedFilter> = _currentFilter.asStateFlow()

    private val _summaries = MutableStateFlow<Map<Long, SummaryState>>(emptyMap())
    val summaries: StateFlow<Map<Long, SummaryState>> = _summaries.asStateFlow()

    private fun isToday(timestamp: Long): Boolean {
        val calendar = java.util.Calendar.getInstance()
        val today = calendar.get(java.util.Calendar.DAY_OF_YEAR)
        val year = calendar.get(java.util.Calendar.YEAR)
        calendar.timeInMillis = timestamp
        return calendar.get(java.util.Calendar.DAY_OF_YEAR) == today && 
               calendar.get(java.util.Calendar.YEAR) == year
    }

    val todayArticleCount: StateFlow<Int> = articleDao.getArticlesForFeed()
        .map { list ->
            list.count { isToday(it.article.fetchedAt) }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    val articles: StateFlow<List<ArticleWithSource>> = combine(
        articleDao.getArticlesForFeed(),
        _currentFilter
    ) { articleList, filter ->
        var filteredList = articleList

        // Apply topic filter
        if (filter.topicTag != null) {
            filteredList = filteredList.filter { it.article.topicTag == filter.topicTag }
        }

        // Apply sort order
        when (filter.sortOrder) {
            SortOrder.Newest -> filteredList.sortedByDescending { it.article.publishedAt }
            SortOrder.Oldest -> filteredList.sortedBy { it.article.publishedAt }
            SortOrder.MostCovered -> filteredList.sortedByDescending { it.article.coverageCount }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun updateFilter(filter: FeedFilter) {
        _currentFilter.value = filter
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                _refreshStatus.value = RefreshStatus.FetchingSources
                val insertedIds = newsRepository.fetchAndStoreArticles()
                
                if (insertedIds.isNotEmpty()) {
                    _refreshStatus.value = RefreshStatus.ScoringArticles
                    claudeRepository.scoreAndTagArticles(insertedIds)
                }

                _refreshStatus.value = RefreshStatus.GeneratingBriefing
                claudeRepository.generateDailyBriefing()
                
                _refreshStatus.value = RefreshStatus.Complete
            } catch (e: Exception) {
                Log.e("FeedViewModel", "Manual refresh failed", e)
            } finally {
                _isRefreshing.value = false
                _refreshStatus.value = RefreshStatus.Idle
            }
        }
    }

    fun toggleSummary(articleId: Long) {
        val currentState = _summaries.value[articleId] ?: SummaryState.Idle
        
        if (currentState is SummaryState.Success) {
            // Collapse
            _summaries.value = _summaries.value.toMutableMap().apply { remove(articleId) }
        } else {
            // Fetch and expand
            viewModelScope.launch {
                _summaries.value = _summaries.value.toMutableMap().apply { 
                    put(articleId, SummaryState.Loading) 
                }
                
                val summary = claudeRepository.getSummaryForArticle(articleId)
                
                _summaries.value = _summaries.value.toMutableMap().apply {
                    if (summary != null) {
                        put(articleId, SummaryState.Success(summary))
                    } else {
                        put(articleId, SummaryState.Error)
                    }
                }
            }
        }
    }

    val latestBriefing: StateFlow<DailyBriefingEntity?> = dailyBriefingDao.getLatestBriefing()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )
}
