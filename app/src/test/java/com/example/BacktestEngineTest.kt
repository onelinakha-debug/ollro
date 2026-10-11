package com.example

import com.example.domain.backtest.BacktestConfig
import com.example.domain.backtest.BacktestEngine
import com.example.domain.backtest.BacktestTrade
import com.example.domain.backtest.TradeStats
import com.example.domain.backtest.wilsonInterval
import com.example.domain.model.Candle
import com.example.domain.model.SignalDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.sin

class BacktestEngineTest {

    /** Deterministic noisy trend: slope per bar plus a wave, so signals flip direction now and then. */
    private fun series(count: Int, slope: Double): List<Candle> {
        var price = 100.0
        return (0 until count).map { i ->
            val open = price
            val close = open + slope + 0.8 * sin(i / 5.0) + 0.3 * sin(i / 1.7)
            price = close
            Candle(
                timestamp = 1_700_000_000_000L + i * 60_000L,
                open = open,
                high = maxOf(open, close) + 0.2,
                low = minOf(open, close) - 0.2,
                close = close,
                volume = 100.0 + (i % 7) * 10.0,
                isClosed = true
            )
        }
    }

    private fun trade(net: Double, gross: Double = net) = BacktestTrade(
        signalIndex = 0, signalTimestamp = 0L, direction = SignalDirection.BUY, compositeScore = 30,
        entryPrice = 100.0, exitPrice = 100.0, grossReturnPct = gross, netReturnPct = net
    )

    @Test
    fun breakEvenWinRateMatchesPayout() {
        assertEquals(0.5, BacktestEngine.breakEvenWinRate(100.0), 1e-9)
        assertEquals(1.0 / 1.9, BacktestEngine.breakEvenWinRate(90.0), 1e-9)
    }

    @Test
    fun wilsonIntervalBehavesForSmallAndLargeSamples() {
        val mid = wilsonInterval(50, 100)
        assertTrue(0.5 in mid)
        assertTrue(mid.endInclusive - mid.start in 0.15..0.22)

        val none = wilsonInterval(0, 10)
        assertEquals(0.0, none.start, 1e-12)
        assertTrue(none.endInclusive > 0.1) // 0/10 wins does not prove a 0% win rate

        val all = wilsonInterval(10, 10)
        assertEquals(1.0, all.endInclusive, 1e-12)
    }

    @Test
    fun tradeStatsComputesWinRateProfitFactorAndDrawdown() {
        val stats = TradeStats.of(listOf(1.0, -2.0, 0.5, -1.0, 3.0).map { trade(it) })

        assertEquals(5, stats.trades)
        assertEquals(3, stats.wins)
        assertEquals(2, stats.losses)
        assertEquals(0.6, stats.winRate!!, 1e-9)
        assertEquals(0.3, stats.avgNetReturnPct!!, 1e-9)
        assertEquals(4.5 / 3.0, stats.profitFactor!!, 1e-9)
        // equity: 1, -1, -0.5, -1.5, 1.5 -> peak 1.0, trough -1.5
        assertEquals(2.5, stats.maxDrawdownPct, 1e-9)
    }

    @Test
    fun tradeStatsHandlesEmptyAndAllWinning() {
        assertEquals(0, TradeStats.of(emptyList()).trades)
        assertNull(TradeStats.of(emptyList()).winRate)

        val allWins = TradeStats.of(listOf(trade(1.0), trade(2.0)))
        assertTrue(allWins.profitFactor!!.isInfinite())
        assertEquals(0.0, allWins.maxDrawdownPct, 1e-12)
    }

    @Test
    fun tradesEnterAtNextOpenExitAtHorizonCloseAndDoNotOverlap() {
        val candles = series(400, slope = 0.25)
        val config = BacktestConfig(horizonCandles = 5, nonOverlapping = true)
        val report = BacktestEngine.run("TEST", candles, config)

        assertTrue("expected some trades on a trending series", report.trades.isNotEmpty())
        var prevIndex = Int.MIN_VALUE / 2
        for (t in report.trades) {
            assertEquals(candles[t.signalIndex + 1].open, t.entryPrice, 1e-12)
            assertEquals(candles[t.signalIndex + 5].close, t.exitPrice, 1e-12)
            assertTrue("trades overlap", t.signalIndex - prevIndex >= 5)
            prevIndex = t.signalIndex
        }
    }

    @Test
    fun resultsDoNotUseFutureCandles() {
        val full = series(400, slope = 0.1)
        val horizon = 5
        val config = BacktestConfig(horizonCandles = horizon)

        val shortRun = BacktestEngine.run("TEST", full.take(250), config)
        val longRun = BacktestEngine.run("TEST", full, config)

        val lastSignalInShort = 250 - 1 - horizon
        val longPrefix = longRun.trades.filter { it.signalIndex <= lastSignalInShort }

        assertTrue(shortRun.trades.isNotEmpty())
        assertEquals("adding future candles changed past signals (look-ahead)", shortRun.trades.size, longPrefix.size)
        for ((a, b) in shortRun.trades.zip(longPrefix)) {
            assertEquals(a.signalIndex, b.signalIndex)
            assertEquals(a.direction, b.direction)
            assertEquals(a.compositeScore, b.compositeScore)
            assertEquals(a.entryPrice, b.entryPrice, 1e-12)
            assertEquals(a.exitPrice, b.exitPrice, 1e-12)
        }
    }

    @Test
    fun costIsSubtractedFromEveryTrade() {
        val candles = series(300, slope = 0.2)
        val report = BacktestEngine.run("TEST", candles, BacktestConfig(horizonCandles = 3, roundTripCostPct = 0.25))

        assertTrue(report.trades.isNotEmpty())
        for (t in report.trades) assertEquals(t.grossReturnPct - 0.25, t.netReturnPct, 1e-12)
    }

    @Test
    fun bearishSignalsProfitFromFallingPrices() {
        val candles = series(400, slope = -0.25)
        val report = BacktestEngine.run("TEST", candles, BacktestConfig(horizonCandles = 5))

        val sells = report.trades.filter { it.direction.isBearish }
        assertTrue("expected SELL signals in a downtrend", sells.isNotEmpty())
        for (t in sells) {
            val raw = (t.exitPrice - t.entryPrice) / t.entryPrice * 100.0
            assertEquals(-raw, t.grossReturnPct, 1e-9) // a short gains when price falls
        }
    }

    @Test
    fun unfinishedCandlesAreNeverTested() {
        val candles = series(200, slope = 0.2)
        val withForming = candles + candles.last().copy(timestamp = candles.last().timestamp + 60_000L, isClosed = false)

        val report = BacktestEngine.run("TEST", withForming, BacktestConfig(horizonCandles = 3))
        assertEquals(200, report.candlesTested)
    }

    @Test
    fun reportContainsBinaryVerdictAndBaseline() {
        val report = BacktestEngine.run("TEST", series(300, slope = 0.1), BacktestConfig(horizonCandles = 3, binaryPayoutPercent = 90.0))

        assertNotNull(report.baselineUpRate)
        assertEquals(1.0 / 1.9, report.binary.breakEvenWinRate, 1e-9)
        assertFalse(report.toText().isBlank())
        // with so few decided trades the confidence interval is wide, so "clear edge" must not be claimed lightly
        if (report.overall.trades < 30) assertFalse(report.binary.edgeIsStatisticallyClear)
    }
}
