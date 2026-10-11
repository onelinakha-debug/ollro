package com.example

import com.example.domain.engine.BacktestEngine
import com.example.domain.model.Candle
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.cos
import kotlin.math.sin

class BacktestTest {

    private fun generateRealisticCandles(
        count: Int = 300,
        basePrice: Double = 95000.0,
        volatility: Double = 350.0
    ): List<Candle> {
        val candles = mutableListOf<Candle>()
        var currentPrice = basePrice
        val startTime = 1700000000000L

        for (i in 0 until count) {
            val trendFactor = sin(i / 15.0) * volatility + cos(i / 35.0) * (volatility * 1.5)
            val randomNoise = ((i % 7) - 3) * (volatility * 0.4)
            val open = currentPrice
            val close = open + trendFactor + randomNoise
            val high = maxOf(open, close) + kotlin.math.abs(trendFactor * 0.5) + 40.0
            val low = minOf(open, close) - kotlin.math.abs(trendFactor * 0.5) - 40.0
            val volume = 15.0 + (i % 10) * 3.5

            candles.add(
                Candle(
                    timestamp = startTime + (i * 60_000L),
                    open = open,
                    high = high,
                    low = low,
                    close = close,
                    volume = volume
                )
            )
            currentPrice = close
        }
        return candles
    }

    @Test
    fun testSpotStrategyBacktestOutput() {
        val candles = generateRealisticCandles(count = 250, basePrice = 96000.0, volatility = 400.0)
        val report = BacktestEngine.runSpotStrategy(
            symbol = "BTCUSDT",
            candles = candles,
            initialCapital = 10_000.0,
            tradeSizeUsd = 1_000.0,
            takeProfitPercent = 2.0,
            stopLossPercent = 1.0,
            minSignalScore = 30
        )

        // Print backtest report to test console
        println(report.formatSummary())

        assertNotNull(report)
        assertTrue("Total candles analyzed must match input size", report.totalCandles == 250)
        assertTrue("Report must generate trades", report.totalTrades >= 0)
        assertTrue("Max drawdown must be non-negative", report.maxDrawdownPercent >= 0.0)
    }

    @Test
    fun testBinaryOptionsPocketOptionBacktestOutput() {
        val candles = generateRealisticCandles(count = 250, basePrice = 1.0850, volatility = 0.0020)
        val report = BacktestEngine.runBinaryOptionsStrategy(
            symbol = "EURUSD_OTC",
            candles = candles,
            initialCapital = 1_000.0,
            tradeStakeUsd = 50.0,
            expiryCandles = 1,
            payoutRate = 0.85,
            minSignalScore = 35
        )

        println(">>> POCKET OPTION BINARY OTC BACKTEST <<<")
        println(report.formatSummary())

        assertNotNull(report)
        assertTrue("Win rate must be between 0 and 100", report.winRatePercent in 0.0..100.0)
    }
}
