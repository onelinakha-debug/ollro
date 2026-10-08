package com.example.domain.model

/**
 * Domain model representing a user's cryptocurrency asset holding.
 */
data class Holding(
    val id: Long = 0,
    val symbol: String,
    val amount: Double,
    val averageBuyPrice: Double,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun calculateInvestedValue(): Double = amount * averageBuyPrice

    fun calculateCurrentValue(currentPrice: Double): Double = amount * currentPrice

    fun calculatePnL(currentPrice: Double): Double = calculateCurrentValue(currentPrice) - calculateInvestedValue()

    fun calculatePnLPercent(currentPrice: Double): Double =
        if (averageBuyPrice > 0) ((currentPrice - averageBuyPrice) / averageBuyPrice) * 100.0 else 0.0

    fun toHoldingWithPnL(currentPrice: Double): HoldingWithPnL {
        val currentValue = calculateCurrentValue(currentPrice)
        val pnl = calculatePnL(currentPrice)
        val pnlPercent = calculatePnLPercent(currentPrice)
        return HoldingWithPnL(
            holding = this,
            currentPrice = currentPrice,
            currentValue = currentValue,
            pnl = pnl,
            pnlPercent = pnlPercent
        )
    }
}

/**
 * Composite model containing holding details with real-time mark-to-market pricing and PnL.
 */
data class HoldingWithPnL(
    val holding: Holding,
    val currentPrice: Double,
    val currentValue: Double,
    val pnl: Double,
    val pnlPercent: Double
) {
    val isProfit: Boolean get() = pnl >= 0.0
}

/**
 * Aggregate portfolio valuation and cumulative performance metrics.
 */
data class PortfolioSummary(
    val totalInvested: Double = 0.0,
    val totalCurrentValue: Double = 0.0,
    val totalPnL: Double = 0.0,
    val totalPnLPercent: Double = 0.0,
    val holdingsWithPnL: List<HoldingWithPnL> = emptyList()
) {
    val isProfit: Boolean get() = totalPnL >= 0.0
}
