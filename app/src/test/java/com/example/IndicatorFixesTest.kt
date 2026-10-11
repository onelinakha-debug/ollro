package com.example

import com.example.domain.engine.SignalAggregator
import com.example.domain.indicators.IndicatorCalculator
import com.example.domain.model.Candle
import com.example.domain.model.MacdOutput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Regression tests for the indicator/signal bugs fixed in the "make signals trustworthy" change. */
class IndicatorFixesTest {

    private fun candlesFrom(closes: List<Double>, spread: Double = 0.3): List<Candle> =
        closes.mapIndexed { i, close ->
            val open = if (i == 0) close else closes[i - 1]
            Candle(
                timestamp = 1_700_000_000_000L + i * 60_000L,
                open = open,
                high = maxOf(open, close) + spread,
                low = minOf(open, close) - spread,
                close = close,
                volume = 100.0 + (i % 5) * 10.0
            )
        }

    private fun fallThenRise(): List<Double> =
        (0 until 40).map { 100.0 - it * 0.75 } + (0 until 70).map { 70.0 + it * 0.7 }

    @Test
    fun emaIsSeededWithSimpleAverageOfFirstPeriod() {
        val prices = (1..10).map { it.toDouble() }
        val ema = IndicatorCalculator.calculateEMA(prices, 5)

        assertEquals(prices.size, ema.size)
        assertEquals(1.0, ema[0], 1e-12)
        assertEquals(1.5, ema[1], 1e-12)
        assertEquals(3.0, ema[4], 1e-12) // SMA(1..5) = 3
        // k = 2/6: next = 6 * k + 3 * (1 - k) = 4
        assertEquals(4.0, ema[5], 1e-12)
    }

    @Test
    fun emaOfConstantSeriesIsThatConstant() {
        val ema = IndicatorCalculator.calculateEMA(List(300) { 42.0 }, 200)
        assertTrue(ema.all { kotlin.math.abs(it - 42.0) < 1e-9 })
    }

    @Test
    fun macdCrossoverFiresOnlyOnTheFlipCandle() {
        val macd = IndicatorCalculator.calculateMACD(fallThenRise())

        val bullishCrosses = macd.indices.filter { macd[it].isBullishCrossover }
        val positiveHistogram = macd.count { it.histogram > 0 }

        assertTrue("expected at least one bullish crossover", bullishCrosses.isNotEmpty())
        assertTrue("crossover must be rare, not every positive-histogram candle", bullishCrosses.size * 5 < positiveHistogram)
        for (i in bullishCrosses) {
            assertTrue(macd[i].histogram > 0)
            assertTrue(macd[i - 1].histogram <= 0)
        }
        // deep into the rally the histogram is positive but there is no fresh crossover
        assertTrue(macd.last().histogram > 0)
        assertFalse(macd.last().isBullishCrossover)
    }

    @Test
    fun macdCrossoverNeedsPreviousHistogram() {
        assertFalse(MacdOutput(macdLine = 1.0, signalLine = 0.5, histogram = 0.5).isBullishCrossover)
        assertTrue(MacdOutput(1.0, 0.5, 0.5, prevHistogram = -0.1).isBullishCrossover)
        assertTrue(MacdOutput(-1.0, -0.5, -0.5, prevHistogram = 0.1).isBearishCrossover)
        assertFalse(MacdOutput(1.0, 0.5, 0.5, prevHistogram = 0.2).isBullishCrossover)
    }

    @Test
    fun signalEngineReachesBullishExpansionBranchInSustainedRally() {
        val candles = candlesFrom(fallThenRise())
        val signal = SignalAggregator.aggregate("TEST", candles, IndicatorCalculator.calculateAll(candles))

        val macdContribution = signal.contributions.first { it.indicatorName.startsWith("MACD") }
        assertEquals("Bullish Expansion", macdContribution.state)
        assertEquals(40, macdContribution.signalScore)
    }

    @Test
    fun supertrendFollowsRallyThenFlipsOnCrash() {
        val closes = (0 until 60).map { 100.0 + it * 0.8 } + (0 until 40).map { 147.2 - it * 2.0 }
        val candles = candlesFrom(closes, spread = 0.2)
        val st = IndicatorCalculator.calculateSupertrend(candles)

        assertTrue(st.isNotEmpty())
        val offset = candles.size - st.size
        val rallyEnd = 59 - offset
        assertTrue("uptrend expected at end of rally", st[rallyEnd].isBullish)
        assertTrue("support sits below price in an uptrend", st[rallyEnd].value < candles[59].close)

        assertFalse("downtrend expected after the crash", st.last().isBullish)
        assertTrue("resistance sits above price in a downtrend", st.last().value > candles.last().close)
    }

    @Test
    fun calculateAllHasEmaWarmupForTwoHundredCandles() {
        val candles = candlesFrom((0 until 500).map { 100.0 + it * 0.1 })
        val all = IndicatorCalculator.calculateAll(candles)

        assertNotNull(all.ema200)
        assertTrue(all.ema9!! > all.ema21!!)
        assertTrue(all.ema21!! > all.ema50!!)
        assertTrue(all.ema50!! > all.ema200!!)
    }
}
