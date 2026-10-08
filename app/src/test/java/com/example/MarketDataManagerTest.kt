package com.example

import com.example.data.local.dao.CandleDao
import com.example.data.local.db.AlertDao
import com.example.data.local.db.AlertEntity
import com.example.data.local.db.CandleEntity
import com.example.data.remote.binance.BinanceApiService
import com.example.data.remote.binance.BinanceDepthResponse
import com.example.data.remote.binance.BinanceTickerResponse
import com.example.data.remote.binance.BinanceWebSocketManager
import com.example.data.repository.AlertRepository
import com.example.data.repository.BinanceRepository
import com.example.domain.manager.MarketDataEvent
import com.example.domain.manager.MarketDataManager
import com.example.domain.model.Candle
import com.example.domain.model.TechnicalIndicators
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MarketDataManagerTest {

    private class FakeBinanceApi : BinanceApiService {
        override suspend fun getKlines(symbol: String, interval: String, limit: Int): List<List<Any>> {
            return listOf(
                listOf(1700000000000L, "96000.0", "96500.0", "95800.0", "96200.0", "25.0", 1700000060000L, "0", 10, "0", "0", "0")
            )
        }

        override suspend fun getTicker24h(symbol: String): BinanceTickerResponse {
            return BinanceTickerResponse(symbol, "1200.0", "1.25", "96200.0", "97000.0", "95000.0", "1000.0", "96000000.0")
        }

        override suspend fun getAllTickers24h(): List<BinanceTickerResponse> {
            return listOf(getTicker24h("BTCUSDT"))
        }

        override suspend fun getOrderBook(symbol: String, limit: Int): BinanceDepthResponse {
            return BinanceDepthResponse(1L, listOf(listOf("96190.0", "1.5")), listOf(listOf("96210.0", "1.2")))
        }
    }

    private class FakeCandleDao : CandleDao {
        override suspend fun insertCandles(candles: List<CandleEntity>) {}
        override suspend fun insertCandle(candle: CandleEntity) {}
        override fun getRecentCandlesBySymbolFlow(symbol: String, limit: Int): Flow<List<CandleEntity>> = flowOf(emptyList())
        override fun getCandlesFlow(symbol: String, interval: String): Flow<List<CandleEntity>> = flowOf(emptyList())
        override fun getRecentCandlesFlow(symbol: String, interval: String, limit: Int): Flow<List<CandleEntity>> = flowOf(emptyList())
        override suspend fun getCandles(symbol: String, interval: String): List<CandleEntity> = emptyList()
        override suspend fun getLatestCandles(symbol: String, interval: String, limit: Int): List<CandleEntity> = emptyList()
        override fun getCandlesInRange(symbol: String, interval: String, startTime: Long, endTime: Long): Flow<List<CandleEntity>> = flowOf(emptyList())
        override suspend fun getCandlesInRangeList(symbol: String, interval: String, startTime: Long, endTime: Long): List<CandleEntity> = emptyList()
        override fun getCandlesWithIndicators(symbol: String, interval: String): Flow<List<CandleEntity>> = flowOf(emptyList())
        override suspend fun getEarliestTimestamp(symbol: String, interval: String): Long? = null
        override suspend fun getLatestTimestamp(symbol: String, interval: String): Long? = null
        override suspend fun updateIndicators(symbol: String, interval: String, timestamp: Long, indicators: TechnicalIndicators) {}
        override suspend fun deleteOldCandles(symbol: String, interval: String, beforeTimestamp: Long): Int = 0
        override suspend fun clearCandles(symbol: String, interval: String) {}
        override suspend fun clearAll() {}
        override suspend fun getCount(): Int = 0
    }

    private class FakeAlertDao : AlertDao {
        override fun getAllAlertsFlow(): Flow<List<AlertEntity>> = flowOf(emptyList())
        override suspend fun getActiveAlerts(): List<AlertEntity> = emptyList()
        override suspend fun insertAlert(alert: AlertEntity): Long = 1L
        override suspend fun updateAlert(alert: AlertEntity) {}
        override suspend fun deleteAlert(alert: AlertEntity) {}
        override suspend fun deleteAlertById(alertId: Long) {}
    }

    @Test
    fun testMarketDataManagerSingleton() {
        val binanceRepo = BinanceRepository(
            apiService = FakeBinanceApi(),
            candleDao = FakeCandleDao(),
            webSocketManager = BinanceWebSocketManager(OkHttpClient())
        )
        val alertRepo = AlertRepository(FakeAlertDao())

        val instance1 = MarketDataManager.getInstance(binanceRepo, alertRepo)
        val instance2 = MarketDataManager.getInstance(binanceRepo, alertRepo)
        val instance3 = MarketDataManager.getInstance()

        assertSame("MarketDataManager must return same singleton instance", instance1, instance2)
        assertSame("MarketDataManager.getInstance() must return singleton instance", instance1, instance3)
    }

    @Test
    fun testSharedFlowStreamExposure() {
        val binanceRepo = BinanceRepository(
            apiService = FakeBinanceApi(),
            candleDao = FakeCandleDao(),
            webSocketManager = BinanceWebSocketManager(OkHttpClient())
        )
        val alertRepo = AlertRepository(FakeAlertDao())

        val manager = MarketDataManager.getInstance(binanceRepo, alertRepo)

        assertNotNull(manager.marketDataStream)
        assertNotNull(manager.candleStream)
        assertNotNull(manager.tickerStream)
        assertNotNull(manager.depthStream)
    }

    @Test
    fun testExponentialBackoffWithJitter() {
        val binanceRepo = BinanceRepository(
            apiService = FakeBinanceApi(),
            candleDao = FakeCandleDao(),
            webSocketManager = BinanceWebSocketManager(OkHttpClient())
        )
        val alertRepo = AlertRepository(FakeAlertDao())
        val manager = MarketDataManager.getInstance(binanceRepo, alertRepo)

        val delay1 = manager.calculateBackoffDelay(1)
        val delay2 = manager.calculateBackoffDelay(2)
        val delay3 = manager.calculateBackoffDelay(3)
        val delayLarge = manager.calculateBackoffDelay(15)

        assertTrue("Delay 1 should be >= 1000ms", delay1 >= 1000L)
        assertTrue("Delay 2 should be >= 1500ms", delay2 >= 1500L)
        assertTrue("Delay 3 should be >= 2000ms", delay3 >= 2000L)
        assertTrue("Delay should not exceed MAX_BACKOFF_MS", delayLarge <= MarketDataManager.MAX_BACKOFF_MS)
    }

    @Test
    fun testKeepAliveMechanismAndPong() {
        val binanceRepo = BinanceRepository(
            apiService = FakeBinanceApi(),
            candleDao = FakeCandleDao(),
            webSocketManager = BinanceWebSocketManager(OkHttpClient())
        )
        val alertRepo = AlertRepository(FakeAlertDao())
        val manager = MarketDataManager.getInstance(binanceRepo, alertRepo)

        manager.sendPingKeepAlive()
        manager.onPongReceived()
    }

    @Test
    fun testRobustErrorHandling() {
        val binanceRepo = BinanceRepository(
            apiService = FakeBinanceApi(),
            candleDao = FakeCandleDao(),
            webSocketManager = BinanceWebSocketManager(OkHttpClient())
        )
        val alertRepo = AlertRepository(FakeAlertDao())
        val manager = MarketDataManager.getInstance(binanceRepo, alertRepo)

        manager.handleConnectionError(RuntimeException("Simulated socket disconnect"))
        assertNotNull(manager.lastError.value)
        assertTrue(manager.lastError.value!!.contains("Simulated socket disconnect"))
    }
}
