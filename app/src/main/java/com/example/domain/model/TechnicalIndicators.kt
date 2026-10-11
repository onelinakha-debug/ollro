package com.example.domain.model

data class MacdOutput(
    val macdLine: Double,
    val signalLine: Double,
    val histogram: Double,
    /** Histogram value of the previous candle; null when unknown (first candle / legacy data). */
    val prevHistogram: Double? = null
) {
    /**
     * True only on the candle where the MACD line crosses ABOVE the signal line
     * (histogram flips from <= 0 to > 0). It used to be true for every candle with a positive
     * histogram, which made the "crossover" score fire constantly and the "expansion" branch dead.
     */
    val isBullishCrossover: Boolean
        get() = prevHistogram != null && prevHistogram <= 0.0 && histogram > 0.0

    /** True only on the candle where the MACD line crosses BELOW the signal line. */
    val isBearishCrossover: Boolean
        get() = prevHistogram != null && prevHistogram >= 0.0 && histogram < 0.0
}

data class BollingerBandsOutput(
    val upper: Double,
    val middle: Double,
    val lower: Double,
    val bandwidth: Double,
    val percentB: Double
)

data class SupertrendOutput(
    val value: Double,
    val isBullish: Boolean
)

data class TechnicalIndicators(
    val rsi14: Double? = null,
    val macd: MacdOutput? = null,
    val bollingerBands: BollingerBandsOutput? = null,
    val ema9: Double? = null,
    val ema21: Double? = null,
    val ema50: Double? = null,
    val ema200: Double? = null,
    val atr14: Double? = null,
    val vwap: Double? = null,
    val supertrend: SupertrendOutput? = null,
    val rvol: Double? = null
)
