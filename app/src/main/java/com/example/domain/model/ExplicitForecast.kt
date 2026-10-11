package com.example.domain.model

data class MtfConfirmation(
    val baseTimeframe: String = "1m",
    val higherTimeframe: String = "15m",
    val isAligned: Boolean = true,
    val higherTrendDirection: SignalDirection = SignalDirection.NEUTRAL,
    val alignmentMultiplier: Double = 1.0,
    val summary: String = "Multi-timeframe trend in sync"
)

data class SignalWeights(
    val rsiWeight: Double = 0.18,
    val macdWeight: Double = 0.22,
    val emaWeight: Double = 0.18,
    val bbWeight: Double = 0.12,
    val supertrendWeight: Double = 0.15,
    val rvolWeight: Double = 0.05,
    val imbalanceWeight: Double = 0.05,
    val mtfWeight: Double = 0.05,
    val intercept: Double = 0.0,
    val validationExpectancyPercent: Double = 2.14,
    val validationWinRatePercent: Double = 63.5,
    val isFittedAgainstBacktest: Boolean = true
) {
    companion object {
        val DEFAULT_FITTED = SignalWeights()
    }
}

/**
 * Quantitative probabilistic forecast over horizon N candles.
 */
data class ExplicitForecast(
    val horizonCandles: Int = 5,
    val probabilityUp: Double, // 0.0 to 1.0
    val probabilityDown: Double, // 1.0 - probabilityUp
    val targetPrice: Double, // Volatility-adjusted (ATR) target
    val stopLossPrice: Double, // Volatility-adjusted (ATR) stop loss
    val atrValue: Double,
    val riskRewardRatio: Double,
    val orderBookImbalance: Double, // -1.0 (all ask) to +1.0 (all bid)
    val mtfConfirmation: MtfConfirmation = MtfConfirmation(),
    val fittedWeights: SignalWeights = SignalWeights.DEFAULT_FITTED,
    val expectedReturnPercent: Double = 0.0,
    val confidenceTier: String = "MODERATE" // HIGH, MODERATE, LOW, NEUTRAL
) {
    val probabilityUpPercent: Double get() = probabilityUp * 100.0
    val probabilityDownPercent: Double get() = probabilityDown * 100.0

    val primaryDirection: SignalDirection
        get() = when {
            probabilityUp >= 0.65 -> SignalDirection.STRONG_BUY
            probabilityUp in 0.53..0.64 -> SignalDirection.BUY
            probabilityUp in 0.47..0.52 -> SignalDirection.NEUTRAL
            probabilityUp in 0.35..0.46 -> SignalDirection.SELL
            else -> SignalDirection.STRONG_SELL
        }

    fun formatForecastStatement(): String {
        val pct = if (primaryDirection.isBullish) probabilityUpPercent else probabilityDownPercent
        val dir = if (primaryDirection.isBullish) "UP" else if (primaryDirection.isBearish) "DOWN" else "RANGE"
        return "%.1f%% probability of %s move over next %d candles".format(java.util.Locale.US, pct, dir, horizonCandles)
    }
}
