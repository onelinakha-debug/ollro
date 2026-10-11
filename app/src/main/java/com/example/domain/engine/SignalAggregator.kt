package com.example.domain.engine

import com.example.domain.model.Candle
import com.example.domain.model.ExplicitForecast
import com.example.domain.model.IndicatorContribution
import com.example.domain.model.MarketSignal
import com.example.domain.model.MtfConfirmation
import com.example.domain.model.OrderBook
import com.example.domain.model.SignalDirection
import com.example.domain.model.SignalWeights
import com.example.domain.model.TechnicalIndicators
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Quantitative signal aggregation and forecasting engine.
 * Computes explicit probabilistic forecasts over N candles using fitted backtest weights,
 * multi-timeframe trend confirmation, order book depth imbalance, and ATR-based stops/targets.
 */
object SignalAggregator {

    fun aggregate(
        symbol: String,
        candles: List<Candle>,
        indicators: TechnicalIndicators,
        orderBook: OrderBook? = null,
        higherTimeframeIndicators: TechnicalIndicators? = null,
        horizonCandles: Int = 5,
        weights: SignalWeights = SignalWeights.DEFAULT_FITTED
    ): MarketSignal {
        val lastCandle = candles.lastOrNull()
        val close = lastCandle?.close ?: 0.0
        val contributions = mutableListOf<IndicatorContribution>()

        // 1. RSI (14) evaluation
        val rsiVal = indicators.rsi14
        val (rsiScore, rsiState) = when {
            rsiVal == null -> 0 to "Waiting for warm-up"
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
                weight = weights.rsiWeight
            )
        )

        // 2. MACD (12, 26, 9) evaluation with verified crossover
        val macd = indicators.macd
        val (macdScore, macdState) = when {
            macd == null -> 0 to "Waiting for warm-up"
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
                weight = weights.macdWeight
            )
        )

        // 3. EMA Trend Structure (9, 21, 50)
        val ema9 = indicators.ema9
        val ema21 = indicators.ema21
        val ema50 = indicators.ema50
        val (emaScore, emaState) = when {
            ema9 == null || ema21 == null -> 0 to "Insufficient EMA data"
            close > ema9 && ema9 > ema21 -> 75 to "Golden Trend (Price > EMA9 > EMA21)"
            close > ema9 -> 35 to "Moderate Uptrend"
            close < ema9 && ema9 < ema21 -> -75 to "Death Trend (Price < EMA9 < EMA21)"
            close < ema9 -> -35 to "Moderate Downtrend"
            else -> 0 to "Consolidation"
        }
        contributions.add(
            IndicatorContribution(
                indicatorName = "EMA Structure",
                valueFormatted = ema9?.let { String.format(Locale.US, "EMA9: %.2f", it) } ?: "N/A",
                state = emaState,
                signalScore = emaScore,
                weight = weights.emaWeight
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
                weight = weights.bbWeight
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
                valueFormatted = supertrend?.let {
                    String.format(Locale.US, "%.2f (%s)", it.value, if (it.isBullish) "BULL" else "BEAR")
                } ?: "N/A",
                state = stState,
                signalScore = stScore,
                weight = weights.supertrendWeight
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
                weight = weights.rvolWeight
            )
        )

        // 7. Order Book Imbalance Evaluation
        val orderBookImbalance = calculateOrderBookImbalance(orderBook)
        val imbScore = (orderBookImbalance * 80.0).roundToInt().coerceIn(-80, 80)
        val imbState = when {
            orderBookImbalance > 0.25 -> "Heavy Bid Buying Pressure"
            orderBookImbalance < -0.25 -> "Heavy Ask Selling Pressure"
            else -> "Balanced Order Book"
        }
        contributions.add(
            IndicatorContribution(
                indicatorName = "Book Imbalance",
                valueFormatted = String.format(Locale.US, "%+.1f%%", orderBookImbalance * 100.0),
                state = imbState,
                signalScore = imbScore,
                weight = weights.imbalanceWeight
            )
        )

        // 8. Multi-Timeframe Confirmation (MTF)
        val mtfConfirmation = evaluateMtf(indicators, higherTimeframeIndicators)
        val mtfScore = (mtfConfirmation.alignmentMultiplier * (if (mtfConfirmation.isAligned) 40 else -40)).toInt()
        contributions.add(
            IndicatorContribution(
                indicatorName = "MTF Alignment",
                valueFormatted = "${mtfConfirmation.baseTimeframe} vs ${mtfConfirmation.higherTimeframe}",
                state = mtfConfirmation.summary,
                signalScore = mtfScore,
                weight = weights.mtfWeight
            )
        )

        // Calculate Weighted Composite Score
        var totalWeightedScore = 0.0
        for (item in contributions) {
            totalWeightedScore += (item.signalScore * item.weight)
        }
        val compositeScore = totalWeightedScore.roundToInt().coerceIn(-100, 100)

        // Direction Mapping
        val direction = when {
            compositeScore >= 45 -> SignalDirection.STRONG_BUY
            compositeScore in 18..44 -> SignalDirection.BUY
            compositeScore in -17..17 -> SignalDirection.NEUTRAL
            compositeScore in -44..-18 -> SignalDirection.SELL
            else -> SignalDirection.STRONG_SELL
        }

        // Explicit Probabilistic Forecast over N Candles
        // Using Sigmoid calibration with fitted weights:
        val logitZ = (compositeScore / 25.0) + weights.intercept
        val probabilityUp = SignalWeightFitter.sigmoid(logitZ).coerceIn(0.05, 0.95)
        val probabilityDown = 1.0 - probabilityUp

        // ATR-Based Stops and Targets (volatility-adjusted)
        val atr = indicators.atr14 ?: if (close > 0) close * 0.008 else 50.0
        val isBullishTarget = direction.isBullish || (direction == SignalDirection.NEUTRAL && probabilityUp >= 0.50)

        val targetPrice = if (isBullishTarget) {
            close + (1.5 * atr)
        } else {
            close - (1.5 * atr)
        }

        val stopLossPrice = if (isBullishTarget) {
            close - (1.0 * atr)
        } else {
            close + (1.0 * atr)
        }

        val riskRewardRatio = 1.5 // 1.5 ATR target / 1.0 ATR stop
        val atrPct = if (close > 0) (atr / close) * 100.0 else 1.0
        val expectedReturnPercent = if (isBullishTarget) {
            (probabilityUp * 1.5 * atrPct) - (probabilityDown * 1.0 * atrPct)
        } else {
            (probabilityDown * 1.5 * atrPct) - (probabilityUp * 1.0 * atrPct)
        }

        val confidenceTier = when {
            kotlin.math.abs(compositeScore) >= 55 -> "HIGH"
            kotlin.math.abs(compositeScore) >= 30 -> "MODERATE"
            kotlin.math.abs(compositeScore) >= 15 -> "LOW"
            else -> "NEUTRAL"
        }

        val forecast = ExplicitForecast(
            horizonCandles = horizonCandles,
            probabilityUp = probabilityUp,
            probabilityDown = probabilityDown,
            targetPrice = targetPrice,
            stopLossPrice = stopLossPrice,
            atrValue = atr,
            riskRewardRatio = riskRewardRatio,
            orderBookImbalance = orderBookImbalance,
            mtfConfirmation = mtfConfirmation,
            fittedWeights = weights,
            expectedReturnPercent = expectedReturnPercent,
            confidenceTier = confidenceTier
        )

        // Indicator consensus confidence
        val alignedCount = contributions.count {
            if (compositeScore >= 18) it.signalScore > 0
            else if (compositeScore <= -18) it.signalScore < 0
            else kotlin.math.abs(it.signalScore) < 25
        }
        val baseConfidence = ((alignedCount.toDouble() / contributions.size) * 100).roundToInt()
        val confidencePercent = (baseConfidence + if (rvol > 1.2) 8 else 0).coerceIn(40, 98)

        val summary = when (direction) {
            SignalDirection.STRONG_BUY -> "High-conviction BUY. Forecast %.1f%% UP over %d bars. ATR target $%,.2f.".format(Locale.US, probabilityUp * 100.0, horizonCandles, targetPrice)
            SignalDirection.BUY -> "Bullish momentum detected. Forecast %.1f%% UP over %d bars with multi-timeframe alignment.".format(Locale.US, probabilityUp * 100.0, horizonCandles)
            SignalDirection.NEUTRAL -> "Consolidation regime. Balanced probabilities (%.1f%% UP / %.1f%% DOWN). Awaiting breakout.".format(Locale.US, probabilityUp * 100.0, probabilityDown * 100.0)
            SignalDirection.SELL -> "Bearish pressure detected. Forecast %.1f%% DOWN over %d bars. Target $%,.2f.".format(Locale.US, probabilityDown * 100.0, horizonCandles, targetPrice)
            SignalDirection.STRONG_SELL -> "High-conviction SELL. Forecast %.1f%% DOWN over %d bars. Breakdown across indicators.".format(Locale.US, probabilityDown * 100.0, horizonCandles)
        }

        return MarketSignal(
            symbol = symbol,
            direction = direction,
            compositeScore = compositeScore,
            confidencePercent = confidencePercent,
            summary = summary,
            contributions = contributions,
            forecast = forecast,
            isClosedCandleOnly = indicators.isCalculatedOnClosedOnly,
            isWarmedUp = indicators.isWarmedUp
        )
    }

    private fun calculateOrderBookImbalance(orderBook: OrderBook?): Double {
        if (orderBook == null) return 0.0
        val totalBids = orderBook.bids.sumOf { it.quantity }
        val totalAsks = orderBook.asks.sumOf { it.quantity }
        val total = totalBids + totalAsks
        return if (total > 0) ((totalBids - totalAsks) / total).coerceIn(-1.0, 1.0) else 0.0
    }

    private fun evaluateMtf(
        lowerIndicators: TechnicalIndicators,
        higherIndicators: TechnicalIndicators?
    ): MtfConfirmation {
        if (higherIndicators == null) {
            return MtfConfirmation(
                baseTimeframe = "1m",
                higherTimeframe = "15m",
                isAligned = true,
                alignmentMultiplier = 1.0,
                summary = "Single timeframe view"
            )
        }

        val higherBullish = (higherIndicators.supertrend?.isBullish == true) ||
                ((higherIndicators.ema9 ?: 0.0) > (higherIndicators.ema21 ?: 0.0))

        val lowerBullish = (lowerIndicators.supertrend?.isBullish == true) ||
                ((lowerIndicators.ema9 ?: 0.0) > (lowerIndicators.ema21 ?: 0.0))

        val isAligned = higherBullish == lowerBullish
        val higherDir = if (higherBullish) SignalDirection.BUY else SignalDirection.SELL

        return MtfConfirmation(
            baseTimeframe = "1m",
            higherTimeframe = "15m",
            isAligned = isAligned,
            higherTrendDirection = higherDir,
            alignmentMultiplier = if (isAligned) 1.20 else 0.75,
            summary = if (isAligned) "Higher timeframe (15m) confirms trend" else "Counter-trend against higher timeframe (15m)"
        )
    }
}
