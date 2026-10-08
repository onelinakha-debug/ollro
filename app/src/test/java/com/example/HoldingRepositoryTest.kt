package com.example

import com.example.data.local.dao.HoldingDao
import com.example.data.local.db.HoldingEntity
import com.example.data.repository.HoldingRepositoryImpl
import com.example.domain.model.Holding
import com.example.domain.repository.HoldingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HoldingRepositoryTest {

    private class FakeHoldingDao : HoldingDao {
        private val storage = mutableListOf<HoldingEntity>()
        private var idCounter = 1L

        override fun getAllHoldingsFlow(): Flow<List<HoldingEntity>> = flowOf(storage.toList())

        override suspend fun getAllHoldings(): List<HoldingEntity> = storage.toList()

        override suspend fun getHoldingBySymbol(symbol: String): HoldingEntity? {
            return storage.firstOrNull { it.symbol.equals(symbol, ignoreCase = true) }
        }

        override suspend fun getHoldingById(id: Long): HoldingEntity? {
            return storage.firstOrNull { it.id == id }
        }

        override suspend fun insertHolding(holding: HoldingEntity): Long {
            storage.removeAll { it.symbol.equals(holding.symbol, ignoreCase = true) }
            val assignedId = if (holding.id == 0L) idCounter++ else holding.id
            storage.add(holding.copy(id = assignedId))
            return assignedId
        }

        override suspend fun insertHoldings(holdings: List<HoldingEntity>) {
            holdings.forEach { insertHolding(it) }
        }

        override suspend fun updateHolding(holding: HoldingEntity) {
            storage.removeAll { it.id == holding.id }
            storage.add(holding)
        }

        override suspend fun deleteHolding(holding: HoldingEntity) {
            storage.removeAll { it.id == holding.id }
        }

        override suspend fun deleteHoldingById(id: Long) {
            storage.removeAll { it.id == id }
        }

        override suspend fun clearAllHoldings() {
            storage.clear()
        }

        override suspend fun getHoldingsCount(): Int = storage.size
    }

    @Test
    fun testHoldingPnLCalculations() {
        val holding = Holding(
            id = 1,
            symbol = "BTCUSDT",
            amount = 0.5,
            averageBuyPrice = 90000.0
        )

        assertEquals(45000.0, holding.calculateInvestedValue(), 0.001)

        // When BTC is at $100,000 (+11.11%)
        val currentPrice = 100000.0
        assertEquals(50000.0, holding.calculateCurrentValue(currentPrice), 0.001)
        assertEquals(5000.0, holding.calculatePnL(currentPrice), 0.001)
        assertEquals(11.111, holding.calculatePnLPercent(currentPrice), 0.01)

        val withPnL = holding.toHoldingWithPnL(currentPrice)
        assertTrue(withPnL.isProfit)
        assertEquals(5000.0, withPnL.pnl, 0.001)
    }

    @Test
    fun testHoldingLossCalculations() {
        val holding = Holding(
            id = 2,
            symbol = "ETHUSDT",
            amount = 10.0,
            averageBuyPrice = 3000.0
        )

        assertEquals(30000.0, holding.calculateInvestedValue(), 0.001)

        // When ETH is at $2,700 (-10%)
        val currentPrice = 2700.0
        assertEquals(27000.0, holding.calculateCurrentValue(currentPrice), 0.001)
        assertEquals(-3000.0, holding.calculatePnL(currentPrice), 0.001)
        assertEquals(-10.0, holding.calculatePnLPercent(currentPrice), 0.001)

        val withPnL = holding.toHoldingWithPnL(currentPrice)
        assertFalse(withPnL.isProfit)
    }

    @Test
    fun testHoldingRepositoryOperationsAndPortfolioSummary() = runBlocking {
        val fakeDao = FakeHoldingDao()
        val repository: HoldingRepository = HoldingRepositoryImpl(fakeDao)

        repository.saveHolding("BTCUSDT", 0.5, 90000.0)
        repository.saveHolding("ETHUSDT", 2.0, 2500.0)

        val holdings = repository.getAllHoldings().first()
        assertEquals(2, holdings.size)

        // Live price feed stream
        val livePrices = flowOf(
            mapOf(
                "BTCUSDT" to 96000.0,
                "ETHUSDT" to 2800.0
            )
        )

        val summary = repository.observePortfolioSummary(livePrices).first()
        assertNotNull(summary)

        // Invested: (0.5 * 90000) + (2.0 * 2500) = 45000 + 5000 = 50000
        assertEquals(50000.0, summary.totalInvested, 0.001)

        // Current: (0.5 * 96000) + (2.0 * 2800) = 48000 + 5600 = 53600
        assertEquals(53600.0, summary.totalCurrentValue, 0.001)

        // Net PnL: 53600 - 50000 = +3600 (+7.2%)
        assertEquals(3600.0, summary.totalPnL, 0.001)
        assertEquals(7.2, summary.totalPnLPercent, 0.01)
        assertTrue(summary.isProfit)

        // Test delete
        val btcId = holdings.first { it.symbol == "BTCUSDT" }.id
        repository.deleteHolding(btcId)
        assertEquals(1, repository.getHoldingsCount())
    }
}
