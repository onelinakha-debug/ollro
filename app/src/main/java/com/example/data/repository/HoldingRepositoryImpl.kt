package com.example.data.repository

import com.example.data.local.dao.HoldingDao
import com.example.data.local.db.HoldingEntity
import com.example.domain.model.Holding
import com.example.domain.model.HoldingWithPnL
import com.example.domain.model.PortfolioSummary
import com.example.domain.repository.HoldingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.util.Locale

class HoldingRepositoryImpl(
    private val holdingDao: HoldingDao
) : HoldingRepository {

    override fun getAllHoldings(): Flow<List<Holding>> {
        return holdingDao.getAllHoldingsFlow().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getHoldingsList(): List<Holding> {
        return holdingDao.getAllHoldings().map { it.toDomain() }
    }

    override fun getHoldingBySymbol(symbol: String): Flow<Holding?> {
        val uppercaseSymbol = symbol.uppercase(Locale.US)
        return holdingDao.getAllHoldingsFlow().map { list ->
            list.firstOrNull { it.symbol.equals(uppercaseSymbol, ignoreCase = true) }?.toDomain()
        }
    }

    override suspend fun saveHolding(symbol: String, amount: Double, averageBuyPrice: Double): Long {
        val uppercaseSymbol = symbol.uppercase(Locale.US)
        val existing = holdingDao.getHoldingBySymbol(uppercaseSymbol)
        val entity = if (existing != null) {
            existing.copy(amount = amount, averageBuyPrice = averageBuyPrice)
        } else {
            HoldingEntity(
                symbol = uppercaseSymbol,
                amount = amount,
                averageBuyPrice = averageBuyPrice
            )
        }
        return holdingDao.insertHolding(entity)
    }

    override suspend fun deleteHolding(id: Long) {
        holdingDao.deleteHoldingById(id)
    }

    override suspend fun clearAllHoldings() {
        holdingDao.clearAllHoldings()
    }

    override suspend fun getHoldingsCount(): Int {
        return holdingDao.getHoldingsCount()
    }

    override fun observePortfolioSummary(priceFeed: Flow<Map<String, Double>>): Flow<PortfolioSummary> {
        return combine(getAllHoldings(), priceFeed) { holdings, prices ->
            if (holdings.isEmpty()) {
                return@combine PortfolioSummary()
            }

            var totalInvested = 0.0
            var totalCurrentValue = 0.0

            val holdingsWithPnL = holdings.map { holding ->
                val currentPrice = prices[holding.symbol]
                    ?: prices[holding.symbol.uppercase(Locale.US)]
                    ?: getFallbackPrice(holding.symbol)

                val withPnL = holding.toHoldingWithPnL(currentPrice)
                totalInvested += withPnL.holding.calculateInvestedValue()
                totalCurrentValue += withPnL.currentValue
                withPnL
            }

            val totalPnL = totalCurrentValue - totalInvested
            val totalPnLPercent = if (totalInvested > 0) ((totalCurrentValue - totalInvested) / totalInvested) * 100.0 else 0.0

            PortfolioSummary(
                totalInvested = totalInvested,
                totalCurrentValue = totalCurrentValue,
                totalPnL = totalPnL,
                totalPnLPercent = totalPnLPercent,
                holdingsWithPnL = holdingsWithPnL
            )
        }
    }

    private fun getFallbackPrice(symbol: String): Double {
        return when (symbol.uppercase(Locale.US)) {
            "BTCUSDT", "BTC" -> 96850.0
            "ETHUSDT", "ETH" -> 2840.5
            "SOLUSDT", "SOL" -> 218.4
            "BNBUSDT", "BNB" -> 652.1
            "DOGEUSDT", "DOGE" -> 0.385
            "XRPUSDT", "XRP" -> 2.35
            "ADAUSDT", "ADA" -> 0.78
            "AVAXUSDT", "AVAX" -> 32.50
            else -> 100.0
        }
    }
}
