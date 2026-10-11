package com.example.domain.indicators

import com.example.domain.model.BollingerBandsOutput
import com.example.domain.model.Candle
import com.example.domain.model.MacdOutput
import com.example.domain.model.SupertrendOutput
import com.example.domain.model.TechnicalIndicators
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Domain layer calculator taking raw OHLCV candle data as input
 * and providing methods to compute technical indicators (RSI, MACD, Bollinger Bands, EMA, etc.).
 */
class IndicatorCalculator(
    val candles: List<Candle> = emptyList()
) {

    /**
     * Alternative constructor taking raw primitive OHLCV collections.
     */
    constructor(
        opens: List<Double>,
        highs: List<Double>,
        lows: List<Double>,
        closes: List<Double>,
        volumes: List<Double>,
        timestamps: List<Long> = emptyList()
    ) : this(
        candles = closes.indices.map { i ->
            Candle(
                timestamp = if (i < timestamps.size) timestamps[i] else (i * 60_000L),
                open = if (i < opens.size) opens[i] else closes[i],
                high = if (i < highs.size) highs[i] else closes[i],
                low = if (i < lows.size) lows[i] else closes[i],
                close = closes[i],
                volume = if (i < volumes.size) volumes[i] else 1.0,
                isClosed = true
            )
        }
    )

    private val closePrices: List<Double> by lazy { candles.map { it.close } }

    /**
     * Computes Relative Strength Index (RSI) using Wilder's smoothing.
     */
    fun computeRSI(period: Int = 14): List<Double> {
        return calculateRSI(candles, period)
    }

    /**
     * Computes Moving Average Convergence Divergence (MACD).
     */
    fun computeMACD(fast: Int = 12, slow: Int = 26, signal: Int = 9): List<MacdOutput> {
        return calculateMACD(closePrices, fast, slow, signal)
    }

    /**
     * Computes Bollinger Bands (SMA, upper band, lower band, bandwidth, %B).
     */
    fun computeBollingerBands(period: Int = 20, multiplier: Double = 2.0): List<BollingerBandsOutput> {
        return calculateBollingerBands(closePrices, period, multiplier)
    }

    /**
     * Computes Exponential Moving Average (EMA) for a specific period.
     */
    fun computeEMA(period: Int): List<Double> {
        return calculateEMA(closePrices, period)
    }

    /**
     * Computes Average True Range (ATR).
     */
    fun computeATR(period: Int = 14): List<Double> {
        return calculateATR(candles, period)
    }

    /**
     * Computes Volume Weighted Average Price (VWAP).
     */
    fun computeVWAP(): Double {
        return calculateVWAP(candles)
    }

    /**
     * Computes Relative Volume (RVOL).
     */
    fun computeRVOL(period: Int = 24): Double {
        return calculateRVOL(candles, period)
    }

    /**
     * Computes Supertrend trailing stop support & resistance bands.
     */
    fun computeSupertrend(period: Int = 10, multiplier: Double = 3.0): List<SupertrendOutput> {
        return calculateSupertrend(candles, period, multiplier)
    }

    /**
     * Computes full composite TechnicalIndicators snapshot from current OHLCV input.
     */
    fun computeAll(): TechnicalIndicators {
        return calculateAll(candles)
    }

    companion object {

        /**
         * Calculates full composite indicators snapshot for the given candle list.
         */
        fun calculateAll(candles: List<Candle>): TechnicalIndicators {
            if (candles.isEmpty()) return TechnicalIndicators()

            val closePrices = candles.map { it.close }
            val rsi = calculateRSI(candles, period = 14)
            val macd = calculateMACD(closePrices, fast = 12, slow = 26, signal = 9)
            val bb = calculateBollingerBands(closePrices, period = 20, multiplier = 2.0)
            val ema9List = calculateEMA(closePrices, 9)
            val ema21List = calculateEMA(closePrices, 21)
            val ema50List = calculateEMA(closePrices, 50)
            val ema200List = calculateEMA(closePrices, 200)
            val atr = calculateATR(candles, period = 14)
            val vwap = calculateVWAP(candles)
            val supertrend = calculateSupertrend(candles, period = 10, multiplier = 3.0)
            val rvol = calculateRVOL(candles, period = 24)

            return TechnicalIndicators(
                rsi14 = rsi.lastOrNull(),
                macd = macd.lastOrNull(),
                bollingerBands = bb.lastOrNull(),
                ema9 = ema9List.lastOrNull(),
                ema21 = ema21List.lastOrNull(),
                ema50 = ema50List.lastOrNull(),
                ema200 = ema200List.lastOrNull(),
                atr14 = atr.lastOrNull(),
                vwap = vwap,
                supertrend = supertrend.lastOrNull(),
                rvol = rvol
            )
        }

        /**
         * Exponential Moving Average (EMA)
         */
        fun calculateEMA(prices: List<Double>, period: Int): List<Double> {
            if (prices.isEmpty() || period <= 0) return emptyList()
            val result = ArrayList<Double>(prices.size)
            val k = 2.0 / (period + 1.0)

            // Standard seeding: the first `period` values use the running simple average, so the EMA
            // at index (period - 1) equals the SMA of the first `period` prices. Recursion starts after.
            // (Seeding with prices.first() leaves a long-lived bias, especially for EMA50/EMA200.)
            var runningSum = 0.0
            var prevEma = 0.0
            for (i in prices.indices) {
                if (i < period) {
                    runningSum += prices[i]
                    prevEma = runningSum / (i + 1)
                } else {
                    prevEma = (prices[i] * k) + (prevEma * (1.0 - k))
                }
                result.add(prevEma)
            }
            return result
        }

        /**
         * Relative Strength Index (RSI) with Wilder's Smoothing
         */
        fun calculateRSI(candles: List<Candle>, period: Int = 14): List<Double> {
            if (candles.size <= period) return emptyList()

            val changes = mutableListOf<Double>()
            for (i in 1 until candles.size) {
                changes.add(candles[i].close - candles[i - 1].close)
            }

            var avgGain = 0.0
            var avgLoss = 0.0

            for (i in 0 until period) {
                val change = changes[i]
                if (change > 0) avgGain += change else avgLoss += abs(change)
            }
            avgGain /= period
            avgLoss /= period

            val rsiList = mutableListOf<Double>()
            val initialRs = if (avgLoss == 0.0) 100.0 else avgGain / avgLoss
            val initialRsi = if (avgLoss == 0.0) 100.0 else 100.0 - (100.0 / (1.0 + initialRs))
            rsiList.add(initialRsi)

            for (i in period until changes.size) {
                val change = changes[i]
                val gain = if (change > 0) change else 0.0
                val loss = if (change < 0) abs(change) else 0.0

                avgGain = ((avgGain * (period - 1)) + gain) / period
                avgLoss = ((avgLoss * (period - 1)) + loss) / period

                val rs = if (avgLoss == 0.0) 100.0 else avgGain / avgLoss
                val rsi = if (avgLoss == 0.0) 100.0 else 100.0 - (100.0 / (1.0 + rs))
                rsiList.add(rsi.coerceIn(0.0, 100.0))
            }

            return rsiList
        }

        /**
         * Moving Average Convergence Divergence (MACD)
         */
        fun calculateMACD(
            prices: List<Double>,
            fast: Int = 12,
            slow: Int = 26,
            signal: Int = 9
        ): List<MacdOutput> {
            if (prices.size < slow) return emptyList()

            val fastEma = calculateEMA(prices, fast)
            val slowEma = calculateEMA(prices, slow)

            val macdLine = mutableListOf<Double>()
            for (i in prices.indices) {
                macdLine.add(fastEma[i] - slowEma[i])
            }

            val signalLine = calculateEMA(macdLine, signal)
            val result = mutableListOf<MacdOutput>()

            var prevHistogram: Double? = null
            for (i in prices.indices) {
                val m = macdLine[i]
                val s = signalLine[i]
                val hist = m - s
                result.add(
                    MacdOutput(
                        macdLine = m,
                        signalLine = s,
                        histogram = hist,
                        prevHistogram = prevHistogram
                    )
                )
                prevHistogram = hist
            }

            return result
        }

        /**
         * Bollinger Bands (20, 2)
         */
        fun calculateBollingerBands(
            prices: List<Double>,
            period: Int = 20,
            multiplier: Double = 2.0
        ): List<BollingerBandsOutput> {
            if (prices.size < period) return emptyList()

            val result = mutableListOf<BollingerBandsOutput>()
            for (i in (period - 1) until prices.size) {
                val window = prices.subList(i - period + 1, i + 1)
                val mean = window.average()
                val variance = window.sumOf { (it - mean) * (it - mean) } / period
                val stdDev = sqrt(variance)

                val upper = mean + (multiplier * stdDev)
                val lower = mean - (multiplier * stdDev)
                val bandwidth = if (mean != 0.0) (upper - lower) / mean else 0.0
                val currentPrice = prices[i]
                val percentB = if (upper != lower) (currentPrice - lower) / (upper - lower) else 0.5

                result.add(
                    BollingerBandsOutput(
                        upper = upper,
                        middle = mean,
                        lower = lower,
                        bandwidth = bandwidth,
                        percentB = percentB
                    )
                )
            }
            return result
        }

        /**
         * Average True Range (ATR 14)
         */
        fun calculateATR(candles: List<Candle>, period: Int = 14): List<Double> {
            if (candles.size <= 1) return emptyList()

            val trList = mutableListOf<Double>()
            trList.add(candles[0].high - candles[0].low)

            for (i in 1 until candles.size) {
                val current = candles[i]
                val prevClose = candles[i - 1].close
                val tr1 = current.high - current.low
                val tr2 = abs(current.high - prevClose)
                val tr3 = abs(current.low - prevClose)
                trList.add(max(tr1, max(tr2, tr3)))
            }

            if (trList.size < period) return emptyList()

            var currentAtr = trList.take(period).average()
            val atrResult = mutableListOf<Double>()
            atrResult.add(currentAtr)

            for (i in period until trList.size) {
                currentAtr = ((currentAtr * (period - 1)) + trList[i]) / period
                atrResult.add(currentAtr)
            }

            return atrResult
        }

        /**
         * Volume Weighted Average Price (VWAP)
         */
        fun calculateVWAP(candles: List<Candle>): Double {
            if (candles.isEmpty()) return 0.0
            var cumulativeTypicalPriceVolume = 0.0
            var cumulativeVolume = 0.0

            for (c in candles) {
                val typicalPrice = (c.high + c.low + c.close) / 3.0
                cumulativeTypicalPriceVolume += (typicalPrice * c.volume)
                cumulativeVolume += c.volume
            }

            return if (cumulativeVolume > 0) cumulativeTypicalPriceVolume / cumulativeVolume else candles.last().close
        }

        /**
         * Relative Volume (RVOL)
         */
        fun calculateRVOL(candles: List<Candle>, period: Int = 24): Double {
            if (candles.size < 2) return 1.0
            val currentVolume = candles.last().volume
            val previousCandles = candles.dropLast(1).takeLast(period)
            if (previousCandles.isEmpty()) return 1.0

            val avgVolume = previousCandles.map { it.volume }.average()
            return if (avgVolume > 0) currentVolume / avgVolume else 1.0
        }

        /**
         * Supertrend (10, 3)
         */
        fun calculateSupertrend(
            candles: List<Candle>,
            period: Int = 10,
            multiplier: Double = 3.0
        ): List<SupertrendOutput> {
            val atrValues = calculateATR(candles, period)
            if (atrValues.isEmpty()) return emptyList()

            val offset = candles.size - atrValues.size
            val result = mutableListOf<SupertrendOutput>()

            var inUptrend = true
            var finalUpper = 0.0
            var finalLower = 0.0

            for (i in atrValues.indices) {
                val candleIdx = i + offset
                val candle = candles[candleIdx]
                val atr = atrValues[i]
                val hl2 = (candle.high + candle.low) / 2.0

                val basicUpper = hl2 + (multiplier * atr)
                val basicLower = hl2 - (multiplier * atr)

                if (i == 0) {
                    // First usable bar: bands start at the basic bands, trend assumed up.
                    finalUpper = basicUpper
                    finalLower = basicLower
                } else {
                    val prevClose = candles[candleIdx - 1].close
                    val prevFinalUpper = finalUpper
                    val prevFinalLower = finalLower

                    // Bands only tighten toward price, unless the previous close broke through them.
                    finalUpper = if (basicUpper < prevFinalUpper || prevClose > prevFinalUpper) basicUpper else prevFinalUpper
                    finalLower = if (basicLower > prevFinalLower || prevClose < prevFinalLower) basicLower else prevFinalLower

                    // Trend flips when the close breaks the PREVIOUS bar's opposite band.
                    if (inUptrend && candle.close < prevFinalLower) {
                        inUptrend = false
                    } else if (!inUptrend && candle.close > prevFinalUpper) {
                        inUptrend = true
                    }
                }

                result.add(SupertrendOutput(value = if (inUptrend) finalLower else finalUpper, isBullish = inUptrend))
            }

            return result
        }
    }
}
