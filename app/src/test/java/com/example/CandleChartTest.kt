package com.example

import com.example.domain.model.Candle
import com.example.domain.model.TechnicalIndicators
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CandleChartTest {

    @Test
    fun testCandleBullishAndBearishProperties() {
        val bullCandle = Candle(
            timestamp = 1700000000000L,
            open = 95000.0,
            high = 96000.0,
            low = 94800.0,
            close = 95800.0,
            volume = 15.2
        )
        assertTrue("Candle with close > open must be bullish", bullCandle.isBullish)

        val bearCandle = Candle(
            timestamp = 1700000060000L,
            open = 95800.0,
            high = 95900.0,
            low = 94900.0,
            close = 95100.0,
            volume = 18.4
        )
        assertFalse("Candle with close < open must not be bullish", bearCandle.isBullish)
    }

    @Test
    fun testViewportWindowCalculations() {
        val totalCandles = (1..100).map { i ->
            Candle(
                timestamp = 1700000000000L + (i * 60_000L),
                open = 90000.0 + i,
                high = 90010.0 + i,
                low = 89990.0 + i,
                close = 90005.0 + i,
                volume = 10.0 + (i % 5)
            )
        }

        val visibleCount = 35
        val scrollOffset = 0
        val startIndex = (totalCandles.size - visibleCount - scrollOffset).coerceAtLeast(0)
        val window = totalCandles.drop(startIndex).take(visibleCount)

        assertEquals(visibleCount, window.size)
        assertEquals(totalCandles.last().timestamp, window.last().timestamp)
    }
}
