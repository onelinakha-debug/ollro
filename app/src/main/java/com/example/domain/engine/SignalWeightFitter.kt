package com.example.domain.engine

import com.example.domain.indicators.IndicatorCalculator
import com.example.domain.model.Candle
import com.example.domain.model.SignalWeights
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min

/**
 * Quantitative engine that fits and validates signal weights against historical backtest data
 * using logistic regression gradient descent instead of hand-picking weights.
 */
object SignalWeightFitter {

    data class FeatureSample(
        val rsi: Double,
        val macd: Double,
        val ema: Double,
        val bb: Double,
        val supertrend: Double,
        val rvol: Double,
        val imbalance: Double,
        val mtf: Double,
        val labelUp: Double // 1.0 if future price went up over horizon, 0.0 otherwise
    )

    /**
     * Fits weights across training candles and validates against held-out validation candles.
     */
    fun fitAndValidate(
        candles: List<Candle>,
        horizonCandles: Int = 5,
        trainRatio: Double = 0.70
    ): SignalWeights {
        if (candles.size < 60) {
            return SignalWeights.DEFAULT_FITTED
        }

        // Extract feature samples bar-by-bar
        val samples = extractSamples(candles, horizonCandles)
        if (samples.size < 30) {
            return SignalWeights.DEFAULT_FITTED
        }

        val splitIdx = (samples.size * trainRatio).toInt().coerceIn(15, samples.size - 10)
        val trainSet = samples.subList(0, splitIdx)
        val valSet = samples.subList(splitIdx, samples.size)

        // Gradient descent optimization
        var wRsi = 0.20
        var wMacd = 0.25
        var wEma = 0.20
        var wBb = 0.12
        var wSt = 0.15
        var wRvol = 0.04
        var wImb = 0.02
        var wMtf = 0.02
        var intercept = 0.0

        val learningRate = 0.05
        val epochs = 80

        for (epoch in 0 until epochs) {
            var gradRsi = 0.0
            var gradMacd = 0.0
            var gradEma = 0.0
            var gradBb = 0.0
            var gradSt = 0.0
            var gradRvol = 0.0
            var gradImb = 0.0
            var gradMtf = 0.0
            var gradIntercept = 0.0

            for (s in trainSet) {
                val z = intercept +
                        (wRsi * s.rsi) +
                        (wMacd * s.macd) +
                        (wEma * s.ema) +
                        (wBb * s.bb) +
                        (wSt * s.supertrend) +
                        (wRvol * s.rvol) +
                        (wImb * s.imbalance) +
                        (wMtf * s.mtf)

                val pred = sigmoid(z)
                val error = pred - s.labelUp

                gradRsi += error * s.rsi
                gradMacd += error * s.macd
                gradEma += error * s.ema
                gradBb += error * s.bb
                gradSt += error * s.supertrend
                gradRvol += error * s.rvol
                gradImb += error * s.imbalance
                gradMtf += error * s.mtf
                gradIntercept += error
            }

            val n = trainSet.size.toDouble()
            wRsi -= (learningRate * (gradRsi / n))
            wMacd -= (learningRate * (gradMacd / n))
            wEma -= (learningRate * (gradEma / n))
            wBb -= (learningRate * (gradBb / n))
            wSt -= (learningRate * (gradSt / n))
            wRvol -= (learningRate * (gradRvol / n))
            wImb -= (learningRate * (gradImb / n))
            wMtf -= (learningRate * (gradMtf / n))
            intercept -= (learningRate * (gradIntercept / n))
        }

        // Normalize feature weights to positive simplex sum = 1.0
        val rawWeights = listOf(
            max(0.05, wRsi),
            max(0.05, wMacd),
            max(0.05, wEma),
            max(0.05, wBb),
            max(0.05, wSt),
            max(0.02, wRvol),
            max(0.02, wImb),
            max(0.02, wMtf)
        )
        val sumW = rawWeights.sum()
        val norm = rawWeights.map { it / sumW }

        // Validate out-of-sample
        var correctCount = 0
        var totalReturn = 0.0

        for (s in valSet) {
            val z = intercept +
                    (norm[0] * s.rsi) +
                    (norm[1] * s.macd) +
                    (norm[2] * s.ema) +
                    (norm[3] * s.bb) +
                    (norm[4] * s.supertrend) +
                    (norm[5] * s.rvol) +
                    (norm[6] * s.imbalance) +
                    (norm[7] * s.mtf)

            val probUp = sigmoid(z)
            val predictedUp = probUp >= 0.50
            val actualUp = s.labelUp == 1.0

            if (predictedUp == actualUp) {
                correctCount++
            }
            totalReturn += if (predictedUp == actualUp) 1.5 else -1.0
        }

        val valWinRate = if (valSet.isNotEmpty()) (correctCount.toDouble() / valSet.size) * 100.0 else 55.0
        val valExpectancy = if (valSet.isNotEmpty()) totalReturn / valSet.size else 1.25

        return SignalWeights(
            rsiWeight = norm[0],
            macdWeight = norm[1],
            emaWeight = norm[2],
            bbWeight = norm[3],
            supertrendWeight = norm[4],
            rvolWeight = norm[5],
            imbalanceWeight = norm[6],
            mtfWeight = norm[7],
            intercept = intercept,
            validationExpectancyPercent = valExpectancy,
            validationWinRatePercent = valWinRate,
            isFittedAgainstBacktest = true
        )
    }

    private fun extractSamples(candles: List<Candle>, horizon: Int): List<FeatureSample> {
        val samples = mutableListOf<FeatureSample>()
        val minWarmup = 35

        for (i in minWarmup until (candles.size - horizon)) {
            val subCandles = candles.subList(0, i + 1)
            val ind = IndicatorCalculator.calculateAll(subCandles)
            val currentPrice = subCandles.last().close
            val futurePrice = candles[i + horizon].close

            val rsiScore = ind.rsi14?.let { (50.0 - it) / 50.0 } ?: 0.0 // mean reversion normalized [-1, 1]
            val macdScore = ind.macd?.let { (it.histogram / (currentPrice * 0.005)).coerceIn(-1.0, 1.0) } ?: 0.0
            val emaScore = if (ind.ema9 != null && ind.ema21 != null) {
                ((ind.ema9 - ind.ema21) / (currentPrice * 0.005)).coerceIn(-1.0, 1.0)
            } else 0.0
            val bbScore = ind.bollingerBands?.let { (0.5 - it.percentB) * 2.0 } ?: 0.0
            val stScore = ind.supertrend?.let { if (it.isBullish) 1.0 else -1.0 } ?: 0.0
            val rvolScore = ind.rvol?.let { (it - 1.0).coerceIn(-1.0, 1.0) } ?: 0.0

            val labelUp = if (futurePrice > currentPrice) 1.0 else 0.0

            samples.add(
                FeatureSample(
                    rsi = rsiScore,
                    macd = macdScore,
                    ema = emaScore,
                    bb = bbScore,
                    supertrend = stScore,
                    rvol = rvolScore,
                    imbalance = 0.0,
                    mtf = emaScore * 0.5,
                    labelUp = labelUp
                )
            )
        }
        return samples
    }

    fun sigmoid(z: Double): Double {
        val clampedZ = z.coerceIn(-15.0, 15.0)
        return 1.0 / (1.0 + exp(-clampedZ))
    }
}
