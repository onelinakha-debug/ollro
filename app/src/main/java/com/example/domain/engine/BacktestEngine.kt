package com.example.domain.engine

import com.example.domain.indicators.IndicatorCalculator
import com.example.domain.model.Candle
import com.example.domain.model.SignalDirection
import com.example.domain.model.SignalWeights
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Model representing a completed backtest trade.
 */
data class BacktestTrade(
    val tradeId: Int,
    val symbol: String,
    val direction: SignalDirection,
    val entryTime: Long,
    val entryPrice: Double,
    val exitTime: Long,
    val exitPrice: Double,
    val sizeUsd: Double,
    val pnlUsd: Double,
    val pnlPercent: Double,
    val isWin: Boolean,
    val exitReason: String,
    val forecastProbability: Double = 0.50
)

/**
 * Quantitative backtest performance summary with mathematical expectancy and calibration tracking.
 */
data class BacktestReport(
    val symbol: String,
    val timeframe: String = "1m",
    val totalCandles: Int,
    val initialCapital: Double,
    val finalCapital: Double,
    val netProfitUsd: Double,
    val netProfitPercent: Double,
    val totalTrades: Int,
    val winningTrades: Int,
    val losingTrades: Int,
    val winRatePercent: Double,
    val expectancyUsd: Double = 0.0,
    val expectancyPercent: Double = 0.0,
    val profitFactor: Double,
    val maxDrawdownPercent: Double,
    val averageTradePnlPercent: Double,
    val bestTradePercent: Double,
    val worstTradePercent: Double,
    val averageRMultiple: Double = 1.0,
    val calibratedAccuracyPercent: Double = winRatePercent,
    val trades: List<BacktestTrade>
) {
    fun formatSummary(): String = buildString {
        appendLine("=========================================================")
        appendLine("           OLRO QUANTITATIVE BACKTEST REPORT             ")
        appendLine("=========================================================")
        appendLine(" Symbol / Timeframe:      $symbol [$timeframe]")
        appendLine(" Analyzed Closed Candles: $totalCandles bars")
        appendLine(" Initial Capital:         ${String.format(Locale.US, "$%,.2f", initialCapital)}")
        appendLine(" Final Capital:           ${String.format(Locale.US, "$%,.2f", finalCapital)}")
        appendLine(" Net Return:              ${String.format(Locale.US, "%s$%,.2f (%+.2f%%)", if (netProfitUsd >= 0) "+" else "", netProfitUsd, netProfitPercent)}")
        appendLine("---------------------------------------------------------")
        appendLine(" Total Trades Executed:   $totalTrades")
        appendLine(" Win / Loss:              $winningTrades Wins / $losingTrades Losses")
        appendLine(" Win Rate:                ${String.format(Locale.US, "%.2f%%", winRatePercent)}")
        appendLine(" Mathematical Expectancy: ${String.format(Locale.US, "%+$,.2f (%+.2f%% per trade)", expectancyUsd, expectancyPercent)}")
        appendLine(" Profit Factor:           ${String.format(Locale.US, "%.2f", profitFactor)}")
        appendLine(" Max Drawdown:            ${String.format(Locale.US, "%.2f%%", maxDrawdownPercent)}")
        appendLine(" Average Trade Return:    ${String.format(Locale.US, "%+.2f%%", averageTradePnlPercent)}")
        appendLine(" Prediction Accuracy:     ${String.format(Locale.US, "%.1f%% alignment", calibratedAccuracyPercent)}")
        appendLine(" Best Trade:              ${String.format(Locale.US, "%+.2f%%", bestTradePercent)}")
        appendLine(" Worst Trade:             ${String.format(Locale.US, "%+.2f%%", worstTradePercent)}")
        appendLine("=========================================================")
    }
}

/**
 * Algorithmic backtesting engine for Olro trading strategies.
 * Runs non-repainting bar-by-bar walk forward evaluation on closed candles.
 */
object BacktestEngine {

    fun runSpotStrategy(
        symbol: String,
        candles: List<Candle>,
        timeframe: String = "1m",
        initialCapital: Double = 10_000.0,
        tradeSizeUsd: Double = 1_000.0,
        takeProfitPercent: Double = 2.0,
        stopLossPercent: Double = 1.0,
        minSignalScore: Int = 30,
        weights: SignalWeights = SignalWeights.DEFAULT_FITTED
    ): BacktestReport {
        val closedOnly = candles.filter { it.isClosed }
        val evalCandles = if (closedOnly.size >= 40) closedOnly else candles

        if (evalCandles.size < 40) {
            return BacktestReport(
                symbol = symbol,
                timeframe = timeframe,
                totalCandles = evalCandles.size,
                initialCapital = initialCapital,
                finalCapital = initialCapital,
                netProfitUsd = 0.0,
                netProfitPercent = 0.0,
                totalTrades = 0,
                winningTrades = 0,
                losingTrades = 0,
                winRatePercent = 0.0,
                expectancyUsd = 0.0,
                expectancyPercent = 0.0,
                profitFactor = 1.0,
                maxDrawdownPercent = 0.0,
                averageTradePnlPercent = 0.0,
                bestTradePercent = 0.0,
                worstTradePercent = 0.0,
                trades = emptyList()
            )
        }

        var capital = initialCapital
        var peakCapital = initialCapital
        var maxDrawdown = 0.0
        var grossProfit = 0.0
        var grossLoss = 0.0

        val completedTrades = mutableListOf<BacktestTrade>()
        var tradeIdCounter = 1

        var inPosition = false
        var activeDirection = SignalDirection.NEUTRAL
        var entryPrice = 0.0
        var entryTime = 0L
        var forecastProb = 0.50

        val minWarmup = 35
        for (i in minWarmup until evalCandles.size) {
            val subCandles = evalCandles.subList(0, i + 1)
            val currentCandle = subCandles.last()
            val indicators = IndicatorCalculator.calculateAll(subCandles, useClosedOnly = true)
            val signal = SignalAggregator.aggregate(
                symbol = symbol,
                candles = subCandles,
                indicators = indicators,
                weights = weights
            )

            if (inPosition) {
                val currentPrice = currentCandle.close
                val isLong = (activeDirection == SignalDirection.BUY || activeDirection == SignalDirection.STRONG_BUY)

                val priceChangePct = if (isLong) {
                    ((currentPrice - entryPrice) / entryPrice) * 100.0
                } else {
                    ((entryPrice - currentPrice) / entryPrice) * 100.0
                }

                var shouldExit = false
                var exitReason = ""

                if (priceChangePct >= takeProfitPercent) {
                    shouldExit = true
                    exitReason = "TAKE_PROFIT"
                } else if (priceChangePct <= -stopLossPercent) {
                    shouldExit = true
                    exitReason = "STOP_LOSS"
                } else if (isLong && (signal.direction == SignalDirection.SELL || signal.direction == SignalDirection.STRONG_SELL)) {
                    shouldExit = true
                    exitReason = "REVERSAL"
                } else if (!isLong && (signal.direction == SignalDirection.BUY || signal.direction == SignalDirection.STRONG_BUY)) {
                    shouldExit = true
                    exitReason = "REVERSAL"
                }

                if (shouldExit) {
                    val pnlUsd = tradeSizeUsd * (priceChangePct / 100.0)
                    capital += pnlUsd
                    peakCapital = max(peakCapital, capital)

                    val currentDrawdown = if (peakCapital > 0) ((peakCapital - capital) / peakCapital) * 100.0 else 0.0
                    maxDrawdown = max(maxDrawdown, currentDrawdown)

                    if (pnlUsd > 0) grossProfit += pnlUsd else grossLoss += abs(pnlUsd)

                    completedTrades.add(
                        BacktestTrade(
                            tradeId = tradeIdCounter++,
                            symbol = symbol,
                            direction = activeDirection,
                            entryTime = entryTime,
                            entryPrice = entryPrice,
                            exitTime = currentCandle.timestamp,
                            exitPrice = currentPrice,
                            sizeUsd = tradeSizeUsd,
                            pnlUsd = pnlUsd,
                            pnlPercent = priceChangePct,
                            isWin = pnlUsd > 0,
                            exitReason = exitReason,
                            forecastProbability = forecastProb
                        )
                    )

                    inPosition = false
                    activeDirection = SignalDirection.NEUTRAL
                }
            } else {
                if (signal.compositeScore >= minSignalScore) {
                    inPosition = true
                    activeDirection = SignalDirection.BUY
                    entryPrice = currentCandle.close
                    entryTime = currentCandle.timestamp
                    forecastProb = signal.forecast?.probabilityUp ?: 0.60
                } else if (signal.compositeScore <= -minSignalScore) {
                    inPosition = true
                    activeDirection = SignalDirection.SELL
                    entryPrice = currentCandle.close
                    entryTime = currentCandle.timestamp
                    forecastProb = signal.forecast?.probabilityDown ?: 0.60
                }
            }
        }

        val totalTrades = completedTrades.size
        val winningTrades = completedTrades.count { it.isWin }
        val losingTrades = totalTrades - winningTrades
        val winRate = if (totalTrades > 0) (winningTrades.toDouble() / totalTrades) * 100.0 else 0.0
        val profitFactor = if (grossLoss > 0) grossProfit / grossLoss else if (grossProfit > 0) 99.0 else 1.0

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

        val netProfitUsd = capital - initialCapital
        val netProfitPercent = (netProfitUsd / initialCapital) * 100.0
        val avgTradePnl = if (totalTrades > 0) completedTrades.map { it.pnlPercent }.average() else 0.0
        val bestTrade = completedTrades.maxOfOrNull { it.pnlPercent } ?: 0.0
        val worstTrade = completedTrades.minOfOrNull { it.pnlPercent } ?: 0.0

        val correctForecasts = completedTrades.count { t ->
            (t.isWin && t.forecastProbability >= 0.50) || (!t.isWin && t.forecastProbability < 0.50)
        }
        val calAccuracy = if (totalTrades > 0) (correctForecasts.toDouble() / totalTrades) * 100.0 else winRate

        return BacktestReport(
            symbol = symbol,
            timeframe = timeframe,
            totalCandles = evalCandles.size,
            initialCapital = initialCapital,
            finalCapital = capital,
            netProfitUsd = netProfitUsd,
            netProfitPercent = netProfitPercent,
            totalTrades = totalTrades,
            winningTrades = winningTrades,
            losingTrades = losingTrades,
            winRatePercent = winRate,
            expectancyUsd = expectancyUsd,
            expectancyPercent = expectancyPct,
            profitFactor = profitFactor,
            maxDrawdownPercent = maxDrawdown,
            averageTradePnlPercent = avgTradePnl,
            bestTradePercent = bestTrade,
            worstTradePercent = worstTrade,
            averageRMultiple = if (stopLossPercent > 0) avgTradePnl / stopLossPercent else 1.0,
            calibratedAccuracyPercent = calAccuracy,
            trades = completedTrades
        )
    }

    /**
     * Executes backtest specifically for binary options (Pocket Option OTC expiry windows).
     */
    fun runBinaryOptionsStrategy(
        symbol: String,
        candles: List<Candle>,
        initialCapital: Double = 1_000.0,
        tradeStakeUsd: Double = 50.0,
        expiryCandles: Int = 1,
        payoutRate: Double = 0.85,
        minSignalScore: Int = 35
    ): BacktestReport {
        if (candles.size < 50) {
            return runSpotStrategy(symbol, candles)
        }

        var capital = initialCapital
        var peakCapital = initialCapital
        var maxDrawdown = 0.0
        var grossProfit = 0.0
        var grossLoss = 0.0

        val completedTrades = mutableListOf<BacktestTrade>()
        var tradeId = 1

        val minWarmup = 35
        var i = minWarmup
        while (i < candles.size - expiryCandles) {
            val subCandles = candles.subList(0, i + 1)
            val currentCandle = subCandles.last()
            val indicators = IndicatorCalculator.calculateAll(subCandles, useClosedOnly = true)
            val signal = SignalAggregator.aggregate(symbol, subCandles, indicators)

            var entryDirection: SignalDirection? = null
            if (signal.compositeScore >= minSignalScore) {
                entryDirection = SignalDirection.BUY
            } else if (signal.compositeScore <= -minSignalScore) {
                entryDirection = SignalDirection.SELL
            }

            if (entryDirection != null) {
                val entryPrice = currentCandle.close
                val expiryCandle = candles[i + expiryCandles]
                val exitPrice = expiryCandle.close

                val isCall = (entryDirection == SignalDirection.BUY)
                val isWin = if (isCall) exitPrice > entryPrice else exitPrice < entryPrice

                val pnlUsd = if (isWin) tradeStakeUsd * payoutRate else -tradeStakeUsd
                val pnlPercent = if (isWin) payoutRate * 100.0 else -100.0

                capital += pnlUsd
                peakCapital = max(peakCapital, capital)

                val currentDrawdown = if (peakCapital > 0) ((peakCapital - capital) / peakCapital) * 100.0 else 0.0
                maxDrawdown = max(maxDrawdown, currentDrawdown)

                if (pnlUsd > 0) grossProfit += pnlUsd else grossLoss += abs(pnlUsd)

                completedTrades.add(
                    BacktestTrade(
                        tradeId = tradeId++,
                        symbol = symbol,
                        direction = entryDirection,
                        entryTime = currentCandle.timestamp,
                        entryPrice = entryPrice,
                        exitTime = expiryCandle.timestamp,
                        exitPrice = exitPrice,
                        sizeUsd = tradeStakeUsd,
                        pnlUsd = pnlUsd,
                        pnlPercent = pnlPercent,
                        isWin = isWin,
                        exitReason = "EXPIRY_${expiryCandles}M",
                        forecastProbability = if (isCall) signal.forecast?.probabilityUp ?: 0.60 else signal.forecast?.probabilityDown ?: 0.60
                    )
                )

                i += expiryCandles
            } else {
                i++
            }
        }

        val totalTrades = completedTrades.size
        val winningTrades = completedTrades.count { it.isWin }
        val losingTrades = totalTrades - winningTrades
        val winRate = if (totalTrades > 0) (winningTrades.toDouble() / totalTrades) * 100.0 else 0.0
        val profitFactor = if (grossLoss > 0) grossProfit / grossLoss else if (grossProfit > 0) 99.0 else 1.0

        val netProfitUsd = capital - initialCapital
        val netProfitPercent = (netProfitUsd / initialCapital) * 100.0
        val avgTradePnl = if (totalTrades > 0) completedTrades.map { it.pnlPercent }.average() else 0.0
        val bestTrade = completedTrades.maxOfOrNull { it.pnlPercent } ?: 0.0
        val worstTrade = completedTrades.minOfOrNull { it.pnlPercent } ?: 0.0

        val winDec = winRate / 100.0
        val lossDec = 1.0 - winDec
        val expectancyUsd = (winDec * (tradeStakeUsd * payoutRate)) - (lossDec * tradeStakeUsd)
        val expectancyPct = (winDec * (payoutRate * 100.0)) - (lossDec * 100.0)

        return BacktestReport(
            symbol = symbol,
            timeframe = "1m",
            totalCandles = candles.size,
            initialCapital = initialCapital,
            finalCapital = capital,
            netProfitUsd = netProfitUsd,
            netProfitPercent = netProfitPercent,
            totalTrades = totalTrades,
            winningTrades = winningTrades,
            losingTrades = losingTrades,
            winRatePercent = winRate,
            expectancyUsd = expectancyUsd,
            expectancyPercent = expectancyPct,
            profitFactor = profitFactor,
            maxDrawdownPercent = maxDrawdown,
            averageTradePnlPercent = avgTradePnl,
            bestTradePercent = bestTrade,
            worstTradePercent = worstTrade,
            averageRMultiple = payoutRate,
            calibratedAccuracyPercent = winRate,
            trades = completedTrades
        )
    }
}
