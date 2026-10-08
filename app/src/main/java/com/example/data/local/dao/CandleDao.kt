package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.db.CandleEntity
import com.example.domain.model.TechnicalIndicators
import kotlinx.coroutines.flow.Flow

@Dao
interface CandleDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCandles(candles: List<CandleEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCandle(candle: CandleEntity)

    /**
     * Fetch recent candles for a specific symbol using a reactive Flow
     */
    @Query("SELECT * FROM candles WHERE symbol = :symbol ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentCandlesBySymbolFlow(symbol: String, limit: Int = 100): Flow<List<CandleEntity>>

    /**
     * Fetch recent candles for a specific symbol and timeframe interval ordered chronologically
     */
    @Query("SELECT * FROM candles WHERE symbol = :symbol AND interval = :interval ORDER BY timestamp ASC")
    fun getCandlesFlow(symbol: String, interval: String): Flow<List<CandleEntity>>

    @Query("SELECT * FROM candles WHERE symbol = :symbol AND interval = :interval ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentCandlesFlow(symbol: String, interval: String = "1m", limit: Int = 100): Flow<List<CandleEntity>>

    @Query("SELECT * FROM candles WHERE symbol = :symbol AND interval = :interval ORDER BY timestamp ASC")
    suspend fun getCandles(symbol: String, interval: String): List<CandleEntity>

    @Query("SELECT * FROM candles WHERE symbol = :symbol AND interval = :interval ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getLatestCandles(symbol: String, interval: String, limit: Int): List<CandleEntity>

    @Query("SELECT * FROM candles WHERE symbol = :symbol AND interval = :interval AND timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp ASC")
    fun getCandlesInRange(symbol: String, interval: String, startTime: Long, endTime: Long): Flow<List<CandleEntity>>

    @Query("SELECT * FROM candles WHERE symbol = :symbol AND interval = :interval AND timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp ASC")
    suspend fun getCandlesInRangeList(symbol: String, interval: String, startTime: Long, endTime: Long): List<CandleEntity>

    @Query("SELECT * FROM candles WHERE symbol = :symbol AND interval = :interval AND indicators IS NOT NULL ORDER BY timestamp ASC")
    fun getCandlesWithIndicators(symbol: String, interval: String): Flow<List<CandleEntity>>

    @Query("SELECT MIN(timestamp) FROM candles WHERE symbol = :symbol AND interval = :interval")
    suspend fun getEarliestTimestamp(symbol: String, interval: String): Long?

    @Query("SELECT MAX(timestamp) FROM candles WHERE symbol = :symbol AND interval = :interval")
    suspend fun getLatestTimestamp(symbol: String, interval: String): Long?

    @Query("UPDATE candles SET indicators = :indicators WHERE symbol = :symbol AND interval = :interval AND timestamp = :timestamp")
    suspend fun updateIndicators(symbol: String, interval: String, timestamp: Long, indicators: TechnicalIndicators)

    @Query("DELETE FROM candles WHERE symbol = :symbol AND interval = :interval AND timestamp < :beforeTimestamp")
    suspend fun deleteOldCandles(symbol: String, interval: String, beforeTimestamp: Long): Int

    @Query("DELETE FROM candles WHERE symbol = :symbol AND interval = :interval")
    suspend fun clearCandles(symbol: String, interval: String)

    @Query("DELETE FROM candles")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM candles")
    suspend fun getCount(): Int
}
