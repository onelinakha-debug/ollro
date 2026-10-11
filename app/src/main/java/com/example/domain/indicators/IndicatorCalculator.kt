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
 * High-precision domain layer technical indicator calculator.
 * Computes RSI (Wilder's), MACD (proper SMA-seeded EMA), Supertrend (Olivier Seban),
 * Bollinger Bands, ATR, VWAP, and RVOL with strict warm-up validation.
 */
class IndicatorCalculator(
    val candles: List<Candle> = emptyList()
) {

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

    fun computeRSI(period: Int = 14): List<Double> = calculateRSI(candles, period)

    fun computeMACD(fast: Int = 12, slow: Int = 26, signal: Int = 9): List<MacdOutput> =
        calculateMACD(closePrices, fast, slow, signal)

    fun computeBollingerBands(period: Int = 20, multiplier: Double = 2.0): List<BollingerBandsOutput> =
        calculateBollingerBands(closePrices, period, multiplier)

    fun computeEMA(period: Int): List<Double> = calculateEMA(closePrices, period)

    fun computeATR(period: Int = 14): List<Double> = calculateATR(candles, period)

    fun computeVWAP(): Double = calculateVWAP(candles)

    fun computeRVOL(period: Int = 24): Double = calculateRVOL(candles, period)

    fun computeSupertrend(period: Int = 10, multiplier: Double = 3.0): List<SupertrendOutput> =
        calculateSupertrend(candles, period, multiplier)

    fun computeAll(): TechnicalIndicators = calculateAll(candles)

    companion object {
        const val MIN_WARMUP_BARS = 35 // 26 slow EMA + 9 signal EMA
        const val RECOMMENDED_WARMUP_BARS = 50

        /**
         * Calculates full composite indicators snapshot.
         * When [useClosedOnly] is true, strips unclosed forming candles to eliminate repainting.
         */
        fun calculateAll(
            candles: List<Candle>,
            useClosedOnly: Boolean = false
        ): TechnicalIndicators {
            val evalCandles = if (useClosedOnly) {
                val closedOnly = candles.filter { it.isClosed }
                if (closedOnly.isNotEmpty()) closedOnly else candles.dropLast(1).ifEmpty { candles }
            } else {
                candles
            }

            if (evalCandles.isEmpty()) {
                return TechnicalIndicators(
                    isWarmedUp = false,
                    warmupProgress = 0.0,
                    closedBarsCount = 0,
                    isCalculatedOnClosedOnly = useClosedOnly
                )
            }

            val count = evalCandles.size
            val isWarmedUp = count >= MIN_WARMUP_BARS
            val warmupProgress = (count.toDouble() / RECOMMENDED_WARMUP_BARS).coerceIn(0.0, 1.0)

            val closePrices = evalCandles.map { it.close }
            val rsi = calculateRSI(evalCandles, period = 14)
            val macd = calculateMACD(closePrices, fast = 12, slow = 26, signal = 9)
            val bb = calculateBollingerBands(closePrices, period = 20, multiplier = 2.0)
            val ema9List = calculateEMA(closePrices, 9)
            val ema21List = calculateEMA(closePrices, 21)
            val ema50List = calculateEMA(closePrices, 50)
            val ema200List = if (count >= 200) calculateEMA(closePrices, 200) else emptyList()
            val atr = calculateATR(evalCandles, period = 14)
            val vwap = calculateVWAP(evalCandles)
            val supertrend = calculateSupertrend(evalCandles, period = 10, multiplier = 3.0)
            val rvol = calculateRVOL(evalCandles, period = 24)

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
                rvol = rvol,
                isWarmedUp = isWarmedUp,
                warmupProgress = warmupProgress,
                closedBarsCount = count,
                isCalculatedOnClosedOnly = useClosedOnly
            )
        }

        /**
         * Exponential Moving Average (EMA) seeded by the SMA of the first [period] bars.
         * Eliminates startup divergence and aligns with TradingView / TA-Lib standards.
         */
        fun calculateEMA(prices: List<Double>, period: Int): List<Double> {
            if (prices.isEmpty() || period <= 0) return emptyList()
            val result = mutableListOf<Double>()
            val k = 2.0 / (period + 1.0)

            if (prices.size < period) {
                var running = prices.first()
                for (price in prices) {
                    running = (price * k) + (running * (1.0 - k))
                    result.add(running)
                }
                return result
            }

            // Seed initial period with SMA
            var sum = 0.0
            for (i in 0 until (period - 1)) {
                sum += prices[i]
                result.add(sum / (i + 1))
            }
            sum += prices[period - 1]
            val smaSeed = sum / period
            result.add(smaSeed)

            var prevEma = smaSeed
            for (i in period until prices.size) {
                val currentEma = (prices[i] * k) + (prevEma * (1.0 - k))
                result.add(currentEma)
                prevEma = currentEma
            }
            return result
        }

        /**
         * Relative Strength Index (RSI) using Wilder's Smoothing.
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
            rsiList.add(initialRsi.coerceIn(0.0, 100.0))

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
         * Moving Average Convergence Divergence (MACD 12, 26, 9).
         * Properly synchronizes the fast and slow EMA series and computes the signal line
         * with previous histogram tracking for reliable crossover detection.
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

            val macdLine = List(prices.size) { i -> fastEma[i] - slowEma[i] }
            val signalLine = calculateEMA(macdLine, signal)

            val result = mutableListOf<MacdOutput>()
            for (i in prices.indices) {
                val m = macdLine[i]
                val s = signalLine[i]
                val h = m - s
                val prevH = if (i > 0) macdLine[i - 1] - signalLine[i - 1] else null
                result.add(
                    MacdOutput(
                        macdLine = m,
                        signalLine = s,
                        histogram = h,
                        previousHistogram = prevH
                    )
                )
            }
            return result
        }

        /**
         * Bollinger Bands (SMA, upper, lower, bandwidth, %B).
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
         * Average True Range (ATR 14).
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
         * Volume Weighted Average Price (VWAP).
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
         * Relative Volume (RVOL).
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
         * Supertrend (10, 3) indicator per Olivier Seban specification.
         * Correctly ratchets trailing stop bands and maintains trend state across bar transitions.
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
            var prevFinalUpper = 0.0
            var prevFinalLower = 0.0

            for (i in atrValues.indices) {
                val candleIdx = i + offset
                val candle = candles[candleIdx]
                val atr = atrValues[i]
                val hl2 = (candle.high + candle.low) / 2.0

                val basicUpper = hl2 + (multiplier * atr)
                val basicLower = hl2 - (multiplier * atr)

                val finalUpper: Double
                val finalLower: Double

                if (i == 0) {
                    finalUpper = basicUpper
                    finalLower = basicLower
                    inUptrend = candle.close >= hl2
                } else {
                    val prevClose = candles[candleIdx - 1].close
                    finalUpper = if (basicUpper < prevFinalUpper || prevClose > prevFinalUpper) {
                        basicUpper
                    } else {
                        prevFinalUpper
                    }

                    finalLower = if (basicLower > prevFinalLower || prevClose < prevFinalLower) {
                        basicLower
                    } else {
                        prevFinalLower
                    }

                    if (inUptrend) {
                        if (candle.close < finalLower) {
                            inUptrend = false
                        }
                    } else {
                        if (candle.close > finalUpper) {
                            inUptrend = true
                        }
                    }
                }

                val supertrendValue = if (inUptrend) finalLower else finalUpper
                result.add(
                    SupertrendOutput(
                        value = supertrendValue,
                        isBullish = inUptrend,
                        upperBand = finalUpper,
                        lowerBand = finalLower
                    )
                )

                prevFinalUpper = finalUpper
                prevFinalLower = finalLower
            }

            return result
        }
    }
}
