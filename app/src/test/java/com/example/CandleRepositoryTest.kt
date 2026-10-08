package com.example

import com.example.data.local.dao.CandleDao
import com.example.data.local.db.CandleEntity
import com.example.data.repository.CandleRepositoryImpl
import com.example.domain.model.Candle
import com.example.domain.model.TechnicalIndicators
import com.example.domain.repository.CandleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CandleRepositoryTest {

    private class FakeCandleDao : CandleDao {
        private val storage = mutableListOf<CandleEntity>()

        override suspend fun insertCandles(candles: List<CandleEntity>) {
            storage.addAll(candles)
        }

        override suspend fun insertCandle(candle: CandleEntity) {
            storage.add(candle)
        }

        override fun getRecentCandlesBySymbolFlow(symbol: String, limit: Int): Flow<List<CandleEntity>> {
            return flowOf(storage.filter { it.symbol == symbol }.takeLast(limit))
        }

        override fun getCandlesFlow(symbol: String, interval: String): Flow<List<CandleEntity>> {
            return flowOf(storage.filter { it.symbol == symbol && it.interval == interval })
        }

        override fun getRecentCandlesFlow(symbol: String, interval: String, limit: Int): Flow<List<CandleEntity>> {
            return flowOf(storage.filter { it.symbol == symbol && it.interval == interval }.takeLast(limit))
        }

        override suspend fun getCandles(symbol: String, interval: String): List<CandleEntity> {
            return storage.filter { it.symbol == symbol && it.interval == interval }
        }

        override suspend fun getLatestCandles(symbol: String, interval: String, limit: Int): List<CandleEntity> {
            return storage.filter { it.symbol == symbol && it.interval == interval }.takeLast(limit)
        }

        override fun getCandlesInRange(symbol: String, interval: String, startTime: Long, endTime: Long): Flow<List<CandleEntity>> {
            return flowOf(storage.filter { it.symbol == symbol && it.interval == interval && it.timestamp in startTime..endTime })
        }

        override suspend fun getCandlesInRangeList(symbol: String, interval: String, startTime: Long, endTime: Long): List<CandleEntity> {
            return storage.filter { it.symbol == symbol && it.interval == interval && it.timestamp in startTime..endTime }
        }

        override fun getCandlesWithIndicators(symbol: String, interval: String): Flow<List<CandleEntity>> {
            return flowOf(storage.filter { it.symbol == symbol && it.interval == interval && it.indicators != null })
        }

        override suspend fun getEarliestTimestamp(symbol: String, interval: String): Long? {
            return storage.filter { it.symbol == symbol && it.interval == interval }.minOfOrNull { it.timestamp }
        }

        override suspend fun getLatestTimestamp(symbol: String, interval: String): Long? {
            return storage.filter { it.symbol == symbol && it.interval == interval }.maxOfOrNull { it.timestamp }
        }

        override suspend fun updateIndicators(symbol: String, interval: String, timestamp: Long, indicators: TechnicalIndicators) {}

        override suspend fun deleteOldCandles(symbol: String, interval: String, beforeTimestamp: Long): Int = 0

        override suspend fun clearCandles(symbol: String, interval: String) {
            storage.removeAll { it.symbol == symbol && it.interval == interval }
        }

        override suspend fun clearAll() {
            storage.clear()
        }

        override suspend fun getCount(): Int = storage.size
    }

    @Test
    fun testCandleRepositoryOperations() = runBlocking {
        val fakeDao = FakeCandleDao()
        val repository: CandleRepository = CandleRepositoryImpl(fakeDao)

        val candle = Candle(
            timestamp = 1700000000000L,
            open = 95000.0,
            high = 95500.0,
            low = 94800.0,
            close = 95200.0,
            volume = 12.0
        )

        repository.saveCandle("BTCUSDT", "1m", candle)

        val flowResult = repository.observeCandles("BTCUSDT", "1m").first()
        assertEquals(1, flowResult.size)
        assertEquals(95000.0, flowResult.first().open, 0.001)
        assertEquals(95200.0, flowResult.first().close, 0.001)

        val count = repository.getCount()
        assertEquals(1, count)
    }
}
