package com.example.domain.repository

import com.example.domain.model.Candle
import com.example.domain.model.TechnicalIndicators
import kotlinx.coroutines.flow.Flow

/**
 * Domain layer repository interface abstracting Candle persistence and market streams.
 */
interface CandleRepository {

    /**
     * Observes real-time and cached candles for a specific symbol and timeframe interval chronologically.
     */
    fun observeCandles(symbol: String, interval: String): Flow<List<Candle>>

    /**
     * Observes recent candles for a specific symbol up to a maximum limit.
     */
    fun observeRecentCandles(symbol: String, limit: Int = 100): Flow<List<Candle>>

    /**
     * Observes a time window slice of candles between [startTime] and [endTime].
     */
    fun observeCandlesInRange(symbol: String, interval: String, startTime: Long, endTime: Long): Flow<List<Candle>>

    /**
     * Retrieves cached candles synchronously for analytical calculations.
     */
    suspend fun getCandles(symbol: String, interval: String): List<Candle>

    /**
     * Retrieves the latest N candles.
     */
    suspend fun getLatestCandles(symbol: String, interval: String, limit: Int): List<Candle>

    /**
     * Persists a batch of historical or incoming candles.
     */
    suspend fun saveCandles(symbol: String, interval: String, candles: List<Candle>)

    /**
     * Persists a single candle with optional precomputed technical indicators.
     */
    suspend fun saveCandle(symbol: String, interval: String, candle: Candle, indicators: TechnicalIndicators? = null)

    /**
     * Updates technical indicators in-place for a specific timestamp candle.
     */
    suspend fun updateIndicators(symbol: String, interval: String, timestamp: Long, indicators: TechnicalIndicators)

    /**
     * Clears cached candles for a specific symbol and interval.
     */
    suspend fun clearCandles(symbol: String, interval: String)

    /**
     * Purges all cached candle data.
     */
    suspend fun clearAll()

    /**
     * Returns total stored candle count.
     */
    suspend fun getCount(): Int
}
