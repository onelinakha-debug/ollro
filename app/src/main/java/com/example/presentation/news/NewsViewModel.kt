package com.example.presentation.news

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.NewsArticle
import com.example.domain.repository.NewsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class NewsViewModel(
    private val newsRepository: NewsRepository
) : ViewModel() {

    private val _selectedSymbol = MutableStateFlow("ALL")
    val selectedSymbol: StateFlow<String> = _selectedSymbol.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    val newsList: StateFlow<List<NewsArticle>> = _selectedSymbol
        .flatMapLatest { symbol ->
            newsRepository.getNewsFlow(symbol)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        refreshNews()
    }

    fun selectSymbol(symbol: String) {
        _selectedSymbol.value = symbol
    }

    fun refreshNews() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                newsRepository.refreshNews(_selectedSymbol.value)
            } finally {
                _isRefreshing.value = false
            }
        }
    }
}
