package com.example.data.repository

import com.example.data.local.dao.CandleDao
import com.example.data.local.db.CandleEntity
import com.example.domain.model.Candle
import com.example.domain.model.TechnicalIndicators
import com.example.domain.repository.CandleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CandleRepositoryImpl(
    private val candleDao: CandleDao
) : CandleRepository {

    override fun observeCandles(symbol: String, interval: String): Flow<List<Candle>> {
        return candleDao.getCandlesFlow(symbol, interval).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun observeRecentCandles(symbol: String, limit: Int): Flow<List<Candle>> {
        return candleDao.getRecentCandlesBySymbolFlow(symbol, limit).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun observeCandlesInRange(
        symbol: String,
        interval: String,
        startTime: Long,
        endTime: Long
    ): Flow<List<Candle>> {
        return candleDao.getCandlesInRange(symbol, interval, startTime, endTime).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getCandles(symbol: String, interval: String): List<Candle> {
        return candleDao.getCandles(symbol, interval).map { it.toDomain() }
    }

    override suspend fun getLatestCandles(symbol: String, interval: String, limit: Int): List<Candle> {
        return candleDao.getLatestCandles(symbol, interval, limit).map { it.toDomain() }
    }

    override suspend fun saveCandles(symbol: String, interval: String, candles: List<Candle>) {
        val entities = candles.map { CandleEntity.fromDomain(symbol, interval, it) }
        candleDao.insertCandles(entities)
    }

    override suspend fun saveCandle(
        symbol: String,
        interval: String,
        candle: Candle,
        indicators: TechnicalIndicators?
    ) {
        candleDao.insertCandle(CandleEntity.fromDomain(symbol, interval, candle, indicators))
    }

    override suspend fun updateIndicators(
        symbol: String,
        interval: String,
        timestamp: Long,
        indicators: TechnicalIndicators
    ) {
        candleDao.updateIndicators(symbol, interval, timestamp, indicators)
    }

    override suspend fun clearCandles(symbol: String, interval: String) {
        candleDao.clearCandles(symbol, interval)
    }

    override suspend fun clearAll() {
        candleDao.clearAll()
    }

    override suspend fun getCount(): Int {
        return candleDao.getCount()
    }
}
