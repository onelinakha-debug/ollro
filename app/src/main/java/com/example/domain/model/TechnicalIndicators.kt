package com.example.domain.model

data class MacdOutput(
    val macdLine: Double,
    val signalLine: Double,
    val histogram: Double
) {
    val isBullishCrossover: Boolean get() = macdLine > signalLine && histogram > 0
    val isBearishCrossover: Boolean get() = macdLine < signalLine && histogram < 0
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
