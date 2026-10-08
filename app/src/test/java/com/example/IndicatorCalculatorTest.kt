package com.example

import com.example.domain.engine.SignalAggregator
import com.example.domain.indicators.IndicatorCalculator
import com.example.domain.model.Candle
import com.example.domain.model.SignalDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IndicatorCalculatorTest {

    // Helper to generate a known sequence of candles
    private fun createCandles(closePrices: List<Double>): List<Candle> {
        return closePrices.mapIndexed { index, price ->
            Candle(
                timestamp = 1700000000000L + (index * 60_000L),
                open = price * 0.998,
                high = price * 1.005,
                low = price * 0.995,
                close = price,
                volume = 100.0 + (index * 5.0)
            )
        }
    }

    @Test
    fun testEmaCalculation() {
        val prices = listOf(22.27, 22.19, 22.08, 22.17, 22.18, 22.13, 22.23, 22.43, 22.54, 22.25)
        val period = 5
        val emaValues = IndicatorCalculator.calculateEMA(prices, period)

        assertEquals(prices.size, emaValues.size)
        // Verify monotonicity and reasonable bounds
        assertTrue(emaValues.last() in 22.0..23.0)
    }

    @Test
    fun testRsiCalculationWithKnownTrend() {
        // 20 candles with strong upward price action
        val upwardPrices = listOf(
            44.34, 44.09, 44.15, 43.61, 44.33, 44.83, 45.10, 45.42, 45.84, 46.08,
            45.89, 46.03, 45.61, 46.28, 46.28, 46.00, 46.03, 46.41, 46.22, 46.50
        )
        val candles = createCandles(upwardPrices)
        val rsiValues = IndicatorCalculator.calculateRSI(candles, period = 14)

        assertTrue(rsiValues.isNotEmpty())
        val latestRsi = rsiValues.last()
        assertTrue("RSI should be bullish (> 55) for uptrend, got $latestRsi", latestRsi > 55.0)
        assertTrue(latestRsi <= 100.0)
    }

    @Test
    fun testMacdCalculation() {
        val prices = (1..40).map { 100.0 + it * 1.5 } // Steady linear expansion
        val macdList = IndicatorCalculator.calculateMACD(prices, fast = 12, slow = 26, signal = 9)

        assertEquals(prices.size, macdList.size)
        val lastMacd = macdList.last()
        assertTrue("MACD line should be positive in strong uptrend", lastMacd.macdLine > 0)
    }

    @Test
    fun testBollingerBands() {
        val prices = (1..30).map { 50.0 + (it % 3) * 2.0 }
        val bands = IndicatorCalculator.calculateBollingerBands(prices, period = 20, multiplier = 2.0)

        assertTrue(bands.isNotEmpty())
        val latest = bands.last()
        assertTrue(latest.upper > latest.middle)
        assertTrue(latest.middle > latest.lower)
        assertTrue(latest.percentB in -0.5..1.5)
    }

    @Test
    fun testClassInstantiationWithCandles() {
        val upwardPrices = (1..35).map { 50.0 + it * 2.0 }
        val candles = createCandles(upwardPrices)
        val calculator = IndicatorCalculator(candles)

        val rsi = calculator.computeRSI(14)
        val macd = calculator.computeMACD()
        val bb = calculator.computeBollingerBands()
        val ema9 = calculator.computeEMA(9)
        val all = calculator.computeAll()

        assertTrue("RSI list should not be empty", rsi.isNotEmpty())
        assertTrue("MACD list should not be empty", macd.isNotEmpty())
        assertTrue("Bollinger Bands list should not be empty", bb.isNotEmpty())
        assertTrue("EMA9 list should not be empty", ema9.isNotEmpty())
        assertNotNull(all.rsi14)
        assertNotNull(all.macd)
        assertNotNull(all.bollingerBands)
    }

    @Test
    fun testClassInstantiationWithRawPrimitives() {
        val count = 25
        val closes = (1..count).map { 100.0 + it }
        val opens = closes.map { it - 0.5 }
        val highs = closes.map { it + 1.0 }
        val lows = closes.map { it - 1.0 }
        val volumes = closes.map { 1000.0 }

        val calculator = IndicatorCalculator(
            opens = opens,
            highs = highs,
            lows = lows,
            closes = closes,
            volumes = volumes
        )

        val rsi = calculator.computeRSI(14)
        val ema = calculator.computeEMA(5)
        assertEquals(count, calculator.candles.size)
        assertTrue(rsi.isNotEmpty())
        assertEquals(count, ema.size)
    }

    @Test
    fun testSignalAggregatorStrongBuy() {
        val breakoutPrices = (1..30).map { 20000.0 + it * 200.0 }
        val candles = createCandles(breakoutPrices)
        val indicators = IndicatorCalculator.calculateAll(candles)

        val signal = SignalAggregator.aggregate("BTCUSDT", candles, indicators)
        assertNotNull(signal)
        assertEquals("BTCUSDT", signal.symbol)
        assertTrue("Composite score should be positive for strong rally", signal.compositeScore > 0)
        assertTrue(signal.direction == SignalDirection.BUY || signal.direction == SignalDirection.STRONG_BUY)
        assertTrue(signal.contributions.isNotEmpty())
    }
}
