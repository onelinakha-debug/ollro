package com.example.domain.repository

import com.example.core.common.DataResult
import com.example.domain.model.NewsArticle
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for real-time cryptocurrency news aggregation and caching.
 */
interface NewsRepository {

    /**
     * Observes real-time news headlines, optionally filtered by a specific cryptocurrency symbol (e.g. BTC, ETH).
     */
    fun getNewsFlow(symbol: String? = null): Flow<List<NewsArticle>>

    /**
     * Refreshes headlines from external REST & RSS APIs and updates the local Room database cache.
     */
    suspend fun refreshNews(symbol: String? = null): DataResult<List<NewsArticle>>
}
