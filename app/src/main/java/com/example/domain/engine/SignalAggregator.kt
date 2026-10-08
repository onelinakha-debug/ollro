package com.example.domain.engine

import com.example.domain.model.Candle
import com.example.domain.model.IndicatorContribution
import com.example.domain.model.MarketSignal
import com.example.domain.model.SignalDirection
import com.example.domain.model.TechnicalIndicators
import java.util.Locale
import kotlin.math.roundToInt

object SignalAggregator {

    fun aggregate(
        symbol: String,
        candles: List<Candle>,
        indicators: TechnicalIndicators
    ): MarketSignal {
        val lastCandle = candles.lastOrNull()
        val contributions = mutableListOf<IndicatorContribution>()

        // 1. RSI (14) evaluation
        val rsiVal = indicators.rsi14
        val (rsiScore, rsiState) = when {
            rsiVal == null -> 0 to "Waiting for data"
            rsiVal < 30.0 -> 80 to "Oversold (Bullish Reversal)"
            rsiVal in 30.0..45.0 -> 35 to "Accumulation Zone"
            rsiVal in 45.0..55.0 -> 0 to "Neutral Range"
            rsiVal in 55.0..70.0 -> -35 to "Distribution Zone"
            else -> -80 to "Overbought (Bearish Reversal)"
        }
        contributions.add(
            IndicatorContribution(
                indicatorName = "RSI (14)",
                valueFormatted = rsiVal?.let { String.format(Locale.US, "%.1f", it) } ?: "N/A",
                state = rsiState,
                signalScore = rsiScore,
                weight = 0.20
            )
        )

        // 2. MACD (12, 26, 9) evaluation
        val macd = indicators.macd
        val (macdScore, macdState) = when {
            macd == null -> 0 to "Waiting for data"
            macd.isBullishCrossover -> 85 to "Bullish Momentum Crossover"
            macd.isBearishCrossover -> -85 to "Bearish Momentum Crossover"
            macd.histogram > 0 -> 40 to "Bullish Expansion"
            macd.histogram < 0 -> -40 to "Bearish Pressure"
            else -> 0 to "Flat Momentum"
        }
        contributions.add(
            IndicatorContribution(
                indicatorName = "MACD (12,26,9)",
                valueFormatted = macd?.let { String.format(Locale.US, "Hist: %.2f", it.histogram) } ?: "N/A",
                state = macdState,
                signalScore = macdScore,
                weight = 0.25
            )
        )

        // 3. EMA Trend Structure (9, 21, 50)
        val close = lastCandle?.close ?: 0.0
        val ema9 = indicators.ema9
        val ema21 = indicators.ema21
        val ema50 = indicators.ema50
        val (emaScore, emaState) = when {
            ema9 == null || ema21 == null -> 0 to "Insufficient EMA data"
            close > ema9 && ema9 > ema21 -> 75 to "Strong Golden Trend (Price > EMA9 > EMA21)"
            close > ema9 -> 35 to "Moderate Uptrend"
            close < ema9 && ema9 < ema21 -> -75 to "Strong Death Trend (Price < EMA9 < EMA21)"
            close < ema9 -> -35 to "Moderate Downtrend"
            else -> 0 to "Consolidation"
        }
        contributions.add(
            IndicatorContribution(
                indicatorName = "EMA Structure",
                valueFormatted = ema9?.let { String.format(Locale.US, "EMA9: %.2f", it) } ?: "N/A",
                state = emaState,
                signalScore = emaScore,
                weight = 0.20
            )
        )

        // 4. Bollinger Bands (20, 2)
        val bb = indicators.bollingerBands
        val (bbScore, bbState) = when {
            bb == null -> 0 to "Insufficient BB data"
            bb.percentB < 0.05 -> 80 to "Piercing Lower Band (Bounce Expected)"
            bb.percentB > 0.95 -> -80 to "Piercing Upper Band (Correction Expected)"
            bb.bandwidth < 0.02 -> 0 to "Extreme Squeeze (Imminent Breakout)"
            bb.percentB > 0.5 -> 20 to "Upper Channel"
            else -> -20 to "Lower Channel"
        }
        contributions.add(
            IndicatorContribution(
                indicatorName = "Bollinger Bands",
                valueFormatted = bb?.let { String.format(Locale.US, "%%B: %.2f", it.percentB) } ?: "N/A",
                state = bbState,
                signalScore = bbScore,
                weight = 0.15
            )
        )

        // 5. Supertrend (10, 3)
        val supertrend = indicators.supertrend
        val (stScore, stState) = when {
            supertrend == null -> 0 to "Insufficient data"
            supertrend.isBullish -> 65 to "Bullish Trailing Support"
            else -> -65 to "Bearish Trailing Resistance"
        }
        contributions.add(
            IndicatorContribution(
                indicatorName = "Supertrend (10,3)",
                valueFormatted = supertrend?.let { String.format(Locale.US, "%.2f (%s)", it.value, if (it.isBullish) "BULL" else "BEAR") } ?: "N/A",
                state = stState,
                signalScore = stScore,
                weight = 0.10
            )
        )

        // 6. Volume Volatility (RVOL)
        val rvol = indicators.rvol ?: 1.0
        val (rvolScore, rvolState) = when {
            rvol > 2.0 -> 30 to "High Relative Volume (>2.0x)"
            rvol > 1.2 -> 15 to "Above Average Volume"
            rvol < 0.6 -> -15 to "Low Liquidity Drift"
            else -> 0 to "Normal Volume"
        }
        contributions.add(
            IndicatorContribution(
                indicatorName = "Relative Volume",
                valueFormatted = String.format(Locale.US, "%.2fx", rvol),
                state = rvolState,
                signalScore = rvolScore,
                weight = 0.10
            )
        )

        // Weighted Composite Score Calculation
        var totalWeightedScore = 0.0
        for (item in contributions) {
            totalWeightedScore += (item.signalScore * item.weight)
        }
        val compositeScore = totalWeightedScore.roundToInt().coerceIn(-100, 100)

        // Direction Mapping
        val direction = when {
            compositeScore >= 50 -> SignalDirection.STRONG_BUY
            compositeScore in 20..49 -> SignalDirection.BUY
            compositeScore in -19..19 -> SignalDirection.NEUTRAL
            compositeScore in -49..-20 -> SignalDirection.SELL
            else -> SignalDirection.STRONG_SELL
        }

        // Confidence calculation: based on indicator consensus alignment
        val alignedCount = contributions.count {
            if (compositeScore >= 20) it.signalScore > 0
            else if (compositeScore <= -20) it.signalScore < 0
            else kotlin.math.abs(it.signalScore) < 25
        }
        val baseConfidence = ((alignedCount.toDouble() / contributions.size) * 100).roundToInt()
        val confidencePercent = (baseConfidence + if (rvol > 1.2) 10 else 0).coerceIn(40, 98)

        val summary = when (direction) {
            SignalDirection.STRONG_BUY -> "High-conviction BUY. Multiple indicators confirm strong upward momentum with volume confirmation."
            SignalDirection.BUY -> "Favorable bullish setup. Momentum and moving averages indicate upside bias."
            SignalDirection.NEUTRAL -> "Market in consolidation or conflicting signals. Recommend waiting for breakout."
            SignalDirection.SELL -> "Bearish bias detected. Moving averages trending downward with fading momentum."
            SignalDirection.STRONG_SELL -> "High-conviction SELL. Breakdown across RSI, MACD, and Supertrend with selling pressure."
        }

        return MarketSignal(
            symbol = symbol,
            direction = direction,
            compositeScore = compositeScore,
            confidencePercent = confidencePercent,
            summary = summary,
            contributions = contributions
        )
    }
}
