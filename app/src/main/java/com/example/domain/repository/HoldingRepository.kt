package com.example.domain.repository

import com.example.domain.model.Holding
import com.example.domain.model.PortfolioSummary
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing user cryptocurrency asset holdings and real-time portfolio valuation.
 */
interface HoldingRepository {

    /**
     * Observes all user asset holdings reactively as a Flow.
     */
    fun getAllHoldings(): Flow<List<Holding>>

    /**
     * Retrieves all user asset holdings synchronously.
     */
    suspend fun getHoldingsList(): List<Holding>

    /**
     * Observes a specific holding by symbol.
     */
    fun getHoldingBySymbol(symbol: String): Flow<Holding?>

    /**
     * Saves or updates a holding.
     */
    suspend fun saveHolding(symbol: String, amount: Double, averageBuyPrice: Double): Long

    /**
     * Deletes a holding by ID.
     */
    suspend fun deleteHolding(id: Long)

    /**
     * Clears all user holdings.
     */
    suspend fun clearAllHoldings()

    /**
     * Returns total holding record count.
     */
    suspend fun getHoldingsCount(): Int

    /**
     * Combines user holdings with live market price feeds to stream real-time PnL and total portfolio valuation.
     */
    fun observePortfolioSummary(priceFeed: Flow<Map<String, Double>>): Flow<PortfolioSummary>
}
