package com.example.domain.model

enum class SignalDirection {
    STRONG_BUY,
    BUY,
    NEUTRAL,
    SELL,
    STRONG_SELL;

    val isBullish: Boolean get() = this == STRONG_BUY || this == BUY
    val isBearish: Boolean get() = this == STRONG_SELL || this == SELL
}

data class IndicatorContribution(
    val indicatorName: String,
    val valueFormatted: String,
    val state: String,
    val signalScore: Int, // between -100 and +100
    val weight: Double // 0.0 to 1.0
)

data class MarketSignal(
    val symbol: String,
    val direction: SignalDirection,
    val compositeScore: Int, // -100 to +100
    val confidencePercent: Int,
    val summary: String,
    val contributions: List<IndicatorContribution>,
    val timestamp: Long = System.currentTimeMillis(),
    val forecast: ExplicitForecast? = null,
    val isClosedCandleOnly: Boolean = true,
    val isWarmedUp: Boolean = true
)
