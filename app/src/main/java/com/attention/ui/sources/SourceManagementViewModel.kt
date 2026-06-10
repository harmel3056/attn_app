package com.attention.ui.sources

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.attention.data.dao.SourceDao
import com.attention.data.entity.SourceEntity
import com.attention.data.remote.FeedFetcher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class ValidationState {
    object Idle : ValidationState()
    object Loading : ValidationState()
    object Success : ValidationState()
    data class Error(val message: String) : ValidationState()
}

@HiltViewModel
class SourceManagementViewModel @Inject constructor(
    private val sourceDao: SourceDao,
    private val feedFetcher: FeedFetcher
) : ViewModel() {

    // Expose sources as a StateFlow
    val sources: StateFlow<List<SourceEntity>> = sourceDao.getAllSources()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _validationState = MutableStateFlow<ValidationState>(ValidationState.Idle)
    val validationState: StateFlow<ValidationState> = _validationState.asStateFlow()

    fun toggleSource(sourceId: Long, isEnabled: Boolean) {
        viewModelScope.launch {
            sourceDao.updateEnabled(sourceId, isEnabled)
        }
    }

    fun deleteSource(sourceId: Long) {
        viewModelScope.launch {
            sourceDao.deleteSource(sourceId)
        }
    }

    fun addCustomSource(feedUrl: String) {
        val trimmedUrl = feedUrl.trim()
        
        if (!trimmedUrl.startsWith("http://") && !trimmedUrl.startsWith("https://")) {
            _validationState.value = ValidationState.Error("URL must start with http:// or https://")
            return
        }

        viewModelScope.launch {
            _validationState.value = ValidationState.Loading
            
            try {
                // Temporary source for fetching
                val tempSource = SourceEntity(name = "Temp", feedUrl = trimmedUrl)
                feedFetcher.fetchFeed(tempSource)
                
                val newSource = SourceEntity(
                    name = trimmedUrl.substringAfter("://").substringBefore("/"),
                    feedUrl = trimmedUrl,
                    isEnabled = true,
                    isCustom = true,
                    lastFetched = 0,
                    faviconUrl = null
                )
                sourceDao.insertSource(newSource)
                _validationState.value = ValidationState.Success
            } catch (e: Exception) {
                _validationState.value = ValidationState.Error("Failed to fetch feed: ${e.message}")
            }
        }
    }

    fun resetValidationState() {
        _validationState.value = ValidationState.Idle
    }
}
