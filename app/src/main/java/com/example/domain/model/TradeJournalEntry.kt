package com.example.domain.model

data class TradeJournalEntry(
    val id: Long = 0,
    val symbol: String,
    val timeframe: String = "1m",
    val direction: SignalDirection = SignalDirection.BUY,
    val entryPrice: Double,
    val exitPrice: Double? = null,
    val sizeUsd: Double,
    val targetPrice: Double? = null,
    val stopLossPrice: Double? = null,
    val pnlUsd: Double? = null,
    val pnlPercent: Double? = null,
    val status: String = "OPEN", // "OPEN", "CLOSED"
    val entryTimestamp: Long = System.currentTimeMillis(),
    val exitTimestamp: Long? = null,
    val forecastProbability: Double? = null,
    val notes: String = "",
    val aiAnalysis: String? = null,
    val strategyTag: String = "ALGO_PREDICTION"
) {
    val isOpen: Boolean get() = status == "OPEN"
    val isClosed: Boolean get() = status == "CLOSED"
    val isWin: Boolean get() = (pnlUsd ?: 0.0) > 0
}

data class JournalPortfolioSummary(
    val totalTrades: Int = 0,
    val openTradesCount: Int = 0,
    val closedTradesCount: Int = 0,
    val winCount: Int = 0,
    val lossCount: Int = 0,
    val winRatePercent: Double = 0.0,
    val totalRealizedPnlUsd: Double = 0.0,
    val averageTradePnlPercent: Double = 0.0,
    val expectancyUsd: Double = 0.0,
    val profitFactor: Double = 1.0
)
