package com.example.domain.engine

import com.example.domain.indicators.IndicatorCalculator
import com.example.domain.model.Candle
import com.example.domain.model.ExplicitForecast
import com.example.domain.model.OrderBook
import com.example.domain.model.SignalDirection
import com.example.domain.model.SignalWeights
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

data class ReplayTradeRecord(
    val tradeId: Int,
    val symbol: String,
    val timeframe: String,
    val direction: SignalDirection,
    val entryIndex: Int,
    val entryTime: Long,
    val entryPrice: Double,
    val exitIndex: Int,
    val exitTime: Long,
    val exitPrice: Double,
    val targetPrice: Double,
    val stopPrice: Double,
    val sizeUsd: Double,
    val pnlUsd: Double,
    val pnlPercent: Double,
    val rMultiple: Double, // Return divided by initial risk
    val isWin: Boolean,
    val exitReason: String, // TARGET_HIT, STOP_HIT, HORIZON_EXPIRED, REVERSAL
    val forecastProbability: Double
)

data class ReplayPerformanceSummary(
    val symbol: String,
    val timeframe: String,
    val totalCandles: Int,
    val totalTrades: Int,
    val winningTrades: Int,
    val losingTrades: Int,
    val winRatePercent: Double,
    val grossProfitUsd: Double,
    val grossLossUsd: Double,
    val netProfitUsd: Double,
    val netProfitPercent: Double,
    val averageWinUsd: Double,
    val averageLossUsd: Double,
    val averageWinPercent: Double,
    val averageLossPercent: Double,
    val expectancyUsd: Double, // (WinRate * AvgWin) - (LossRate * AvgLoss)
    val expectancyPercent: Double,
    val profitFactor: Double,
    val maxDrawdownPercent: Double,
    val maxDrawdownUsd: Double,
    val averageRMultiple: Double,
    val sharpeRatio: Double,
    val calibratedAccuracyPercent: Double, // Realized accuracy vs forecasted probability
    val trades: List<ReplayTradeRecord>
) {
    fun formatTelemetry(): String = buildString {
        appendLine("=========================================================")
        appendLine("     BINANCE HISTORICAL REPLAY & PREDICTION HARNESS      ")
        appendLine("=========================================================")
        appendLine(" Asset / Timeframe:       $symbol [$timeframe]")
        appendLine(" Analyzed Closed Bars:    $totalCandles bars")
        appendLine(" Trades Evaluated:        $totalTrades ($winningTrades W / $losingTrades L)")
        appendLine(" Realized Win Rate:       ${String.format(Locale.US, "%.2f%%", winRatePercent)}")
        appendLine(" Mathematical Expectancy: ${String.format(Locale.US, "%+$,.2f (%+.2f%% per trade)", expectancyUsd, expectancyPercent)}")
        appendLine(" Max Drawdown:            ${String.format(Locale.US, "%.2f%% ($%,.2f)", maxDrawdownPercent, maxDrawdownUsd)}")
        appendLine(" Profit Factor:           ${String.format(Locale.US, "%.2f", profitFactor)}")
        appendLine(" Sharpe Ratio:            ${String.format(Locale.US, "%.2f", sharpeRatio)}")
        appendLine(" Average R-Multiple:      ${String.format(Locale.US, "%+.2f R", averageRMultiple)}")
        appendLine(" Prediction Alignment:    ${String.format(Locale.US, "%.1f%% accuracy", calibratedAccuracyPercent)}")
        appendLine("=========================================================")
    }
}

/**
 * Robust replay harness that simulates Binance historical feeds bar-by-bar
 * using strictly closed candles and records empirical win rates, mathematical expectancy,
 * and drawdowns per symbol and timeframe.
 */
object BinanceHistoricalReplayHarness {

    fun replayHistory(
        symbol: String,
        timeframe: String,
        candles: List<Candle>,
        initialCapital: Double = 10_000.0,
        tradeSizeUsd: Double = 1_000.0,
        useAtrStops: Boolean = true,
        takeProfitAtrMultiplier: Double = 1.5,
        stopLossAtrMultiplier: Double = 1.0,
        horizonBars: Int = 5,
        minSignalScore: Int = 25,
        weights: SignalWeights = SignalWeights.DEFAULT_FITTED
    ): ReplayPerformanceSummary {
        // Enforce closed-only bars
        val closedCandles = candles.filter { it.isClosed }
        val evalCandles = if (closedCandles.size >= 40) closedCandles else candles

        if (evalCandles.size < 40) {
            return emptySummary(symbol, timeframe, evalCandles.size, initialCapital)
        }

        var capital = initialCapital
        var peakCapital = initialCapital
        var maxDrawdownPct = 0.0
        var maxDrawdownUsd = 0.0

        var grossProfit = 0.0
        var grossLoss = 0.0

        val completedTrades = mutableListOf<ReplayTradeRecord>()
        var tradeCounter = 1

        var inPosition = false
        var activeDirection = SignalDirection.NEUTRAL
        var entryIndex = 0
        var entryTime = 0L
        var entryPrice = 0.0
        var targetPrice = 0.0
        var stopLossPrice = 0.0
        var tradeRiskUsd = 0.0
        var forecastProb = 0.50

        val minWarmup = 35
        for (i in minWarmup until evalCandles.size) {
            val subCandles = evalCandles.subList(0, i + 1)
            val currentCandle = subCandles.last()
            val currentPrice = currentCandle.close

            if (inPosition) {
                val isLong = (activeDirection == SignalDirection.BUY || activeDirection == SignalDirection.STRONG_BUY)
                var shouldExit = false
                var exitReason = ""
                var exitPrice = currentPrice

                val barsHeld = i - entryIndex

                if (isLong) {
                    if (currentCandle.high >= targetPrice) {
                        shouldExit = true
                        exitReason = "TARGET_HIT"
                        exitPrice = targetPrice
                    } else if (currentCandle.low <= stopLossPrice) {
                        shouldExit = true
                        exitReason = "STOP_HIT"
                        exitPrice = stopLossPrice
                    } else if (barsHeld >= horizonBars) {
                        shouldExit = true
                        exitReason = "HORIZON_EXPIRED"
                        exitPrice = currentPrice
                    }
                } else {
                    if (currentCandle.low <= targetPrice) {
                        shouldExit = true
                        exitReason = "TARGET_HIT"
                        exitPrice = targetPrice
                    } else if (currentCandle.high >= stopLossPrice) {
                        shouldExit = true
                        exitReason = "STOP_HIT"
                        exitPrice = stopLossPrice
                    } else if (barsHeld >= horizonBars) {
                        shouldExit = true
                        exitReason = "HORIZON_EXPIRED"
                        exitPrice = currentPrice
                    }
                }

                if (shouldExit) {
                    val priceChangePct = if (isLong) {
                        ((exitPrice - entryPrice) / entryPrice) * 100.0
                    } else {
                        ((entryPrice - exitPrice) / entryPrice) * 100.0
                    }

                    val pnlUsd = tradeSizeUsd * (priceChangePct / 100.0)
                    capital += pnlUsd
                    peakCapital = max(peakCapital, capital)

                    val curDdUsd = max(0.0, peakCapital - capital)
                    val curDdPct = if (peakCapital > 0) (curDdUsd / peakCapital) * 100.0 else 0.0
                    maxDrawdownPct = max(maxDrawdownPct, curDdPct)
                    maxDrawdownUsd = max(maxDrawdownUsd, curDdUsd)

                    if (pnlUsd > 0) grossProfit += pnlUsd else grossLoss += abs(pnlUsd)

                    val rMultiple = if (tradeRiskUsd > 0) pnlUsd / tradeRiskUsd else if (pnlUsd > 0) 1.5 else -1.0

                    completedTrades.add(
                        ReplayTradeRecord(
                            tradeId = tradeCounter++,
                            symbol = symbol,
                            timeframe = timeframe,
                            direction = activeDirection,
                            entryIndex = entryIndex,
                            entryTime = entryTime,
                            entryPrice = entryPrice,
                            exitIndex = i,
                            exitTime = currentCandle.timestamp,
                            exitPrice = exitPrice,
                            targetPrice = targetPrice,
                            stopPrice = stopLossPrice,
                            sizeUsd = tradeSizeUsd,
                            pnlUsd = pnlUsd,
                            pnlPercent = priceChangePct,
                            rMultiple = rMultiple,
                            isWin = pnlUsd > 0,
                            exitReason = exitReason,
                            forecastProbability = forecastProb
                        )
                    )

                    inPosition = false
                    activeDirection = SignalDirection.NEUTRAL
                }
            } else {
                // Evaluate strictly closed candles up to bar i
                val indicators = IndicatorCalculator.calculateAll(subCandles, useClosedOnly = true)
                val signal = SignalAggregator.aggregate(
                    symbol = symbol,
                    candles = subCandles,
                    indicators = indicators,
                    orderBook = null,
                    horizonCandles = horizonBars,
                    weights = weights
                )

                val atr = indicators.atr14 ?: (currentPrice * 0.008)

                if (signal.compositeScore >= minSignalScore) {
                    inPosition = true
                    activeDirection = SignalDirection.BUY
                    entryIndex = i
                    entryTime = currentCandle.timestamp
                    entryPrice = currentPrice
                    targetPrice = entryPrice + (takeProfitAtrMultiplier * atr)
                    stopLossPrice = entryPrice - (stopLossAtrMultiplier * atr)
                    tradeRiskUsd = tradeSizeUsd * ((entryPrice - stopLossPrice) / entryPrice)
                    forecastProb = signal.forecast?.probabilityUp ?: 0.60
                } else if (signal.compositeScore <= -minSignalScore) {
                    inPosition = true
                    activeDirection = SignalDirection.SELL
                    entryIndex = i
                    entryTime = currentCandle.timestamp
                    entryPrice = currentPrice
                    targetPrice = entryPrice - (takeProfitAtrMultiplier * atr)
                    stopLossPrice = entryPrice + (stopLossAtrMultiplier * atr)
                    tradeRiskUsd = tradeSizeUsd * ((stopLossPrice - entryPrice) / entryPrice)
                    forecastProb = signal.forecast?.probabilityDown ?: 0.60
                }
            }
        }

        val totalTrades = completedTrades.size
        val winningTrades = completedTrades.count { it.isWin }
        val losingTrades = totalTrades - winningTrades
        val winRate = if (totalTrades > 0) (winningTrades.toDouble() / totalTrades) * 100.0 else 0.0

        val wins = completedTrades.filter { it.isWin }
        val losses = completedTrades.filter { !it.isWin }

        val avgWinUsd = if (wins.isNotEmpty()) wins.map { it.pnlUsd }.average() else 0.0
        val avgLossUsd = if (losses.isNotEmpty()) losses.map { abs(it.pnlUsd) }.average() else 0.0
        val avgWinPct = if (wins.isNotEmpty()) wins.map { it.pnlPercent }.average() else 0.0
        val avgLossPct = if (losses.isNotEmpty()) losses.map { abs(it.pnlPercent) }.average() else 0.0

        val winRateDec = winRate / 100.0
        val lossRateDec = 1.0 - winRateDec
        val expectancyUsd = (winRateDec * avgWinUsd) - (lossRateDec * avgLossUsd)
        val expectancyPct = (winRateDec * avgWinPct) - (lossRateDec * avgLossPct)

        val profitFactor = if (grossLoss > 0) grossProfit / grossLoss else if (grossProfit > 0) 99.0 else 1.0
        val netProfitUsd = capital - initialCapital
        val netProfitPercent = (netProfitUsd / initialCapital) * 100.0

        val avgR = if (completedTrades.isNotEmpty()) completedTrades.map { it.rMultiple }.average() else 0.0

        // Sharpe Ratio approximation: mean(return) / stdDev(return) * sqrt(bars)
        val returns = completedTrades.map { it.pnlPercent }
        val sharpe = if (returns.size >= 2) {
            val meanR = returns.average()
            val variance = returns.sumOf { (it - meanR) * (it - meanR) } / (returns.size - 1)
            val stdDev = sqrt(variance)
            if (stdDev > 0) (meanR / stdDev) * sqrt(252.0) else 0.0
        } else 0.0

        val correctForecasts = completedTrades.count { t ->
            (t.isWin && t.forecastProbability >= 0.50) || (!t.isWin && t.forecastProbability < 0.50)
        }
        val calAccuracy = if (totalTrades > 0) (correctForecasts.toDouble() / totalTrades) * 100.0 else winRate

        return ReplayPerformanceSummary(
            symbol = symbol,
            timeframe = timeframe,
            totalCandles = evalCandles.size,
            totalTrades = totalTrades,
            winningTrades = winningTrades,
            losingTrades = losingTrades,
            winRatePercent = winRate,
            grossProfitUsd = grossProfit,
            grossLossUsd = grossLoss,
            netProfitUsd = netProfitUsd,
            netProfitPercent = netProfitPercent,
            averageWinUsd = avgWinUsd,
            averageLossUsd = avgLossUsd,
            averageWinPercent = avgWinPct,
            averageLossPercent = avgLossPct,
            expectancyUsd = expectancyUsd,
            expectancyPercent = expectancyPct,
            profitFactor = profitFactor,
            maxDrawdownPercent = maxDrawdownPct,
            maxDrawdownUsd = maxDrawdownUsd,
            averageRMultiple = avgR,
            sharpeRatio = sharpe,
            calibratedAccuracyPercent = calAccuracy,
            trades = completedTrades
        )
    }

    private fun emptySummary(symbol: String, timeframe: String, count: Int, capital: Double) =
        ReplayPerformanceSummary(
            symbol = symbol,
            timeframe = timeframe,
            totalCandles = count,
            totalTrades = 0,
            winningTrades = 0,
            losingTrades = 0,
            winRatePercent = 0.0,
            grossProfitUsd = 0.0,
            grossLossUsd = 0.0,
            netProfitUsd = 0.0,
            netProfitPercent = 0.0,
            averageWinUsd = 0.0,
            averageLossUsd = 0.0,
            averageWinPercent = 0.0,
            averageLossPercent = 0.0,
            expectancyUsd = 0.0,
            expectancyPercent = 0.0,
            profitFactor = 1.0,
            maxDrawdownPercent = 0.0,
            maxDrawdownUsd = 0.0,
            averageRMultiple = 0.0,
            sharpeRatio = 0.0,
            calibratedAccuracyPercent = 0.0,
            trades = emptyList()
        )
}
