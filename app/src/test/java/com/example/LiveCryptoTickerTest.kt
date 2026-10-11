package com.example

import com.example.domain.model.Ticker24h
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveCryptoTickerTest {

    @Test
    fun testBullishTrendProperties() {
        val bullishTicker = Ticker24h(
            symbol = "BTCUSDT",
            lastPrice = 96850.0,
            priceChange = 3450.0,
            priceChangePercent = 3.69,
            highPrice24h = 97200.0,
            lowPrice24h = 93100.0,
            volume24h = 45000.0,
            quoteVolume24h = 4300000000.0
        )

        assertTrue("Ticker with positive change percent must be bullish", bullishTicker.isPositive)
        assertEquals(3.69, bullishTicker.priceChangePercent, 0.001)

        val rangeSpan = bullishTicker.highPrice24h - bullishTicker.lowPrice24h
        val positionRatio = (bullishTicker.lastPrice - bullishTicker.lowPrice24h) / rangeSpan
        assertTrue("Position ratio must be between 0 and 1", positionRatio in 0.0..1.0)
        assertTrue("High price must exceed low price", bullishTicker.highPrice24h > bullishTicker.lowPrice24h)
    }

    @Test
    fun testBearishTrendProperties() {
        val bearishTicker = Ticker24h(
            symbol = "ETHUSDT",
            lastPrice = 2840.0,
            priceChange = -120.0,
            priceChangePercent = -4.05,
            highPrice24h = 3010.0,
            lowPrice24h = 2810.0,
            volume24h = 120000.0,
            quoteVolume24h = 350000000.0
        )

        assertFalse("Ticker with negative change percent must not be positive", bearishTicker.isPositive)
        assertEquals(-4.05, bearishTicker.priceChangePercent, 0.001)
        assertTrue(bearishTicker.priceChange < 0)
    }
}
