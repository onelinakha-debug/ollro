package com.example.domain.engine

import com.example.domain.model.Candle
import com.example.domain.model.MarketSignal
import com.example.domain.model.SignalDirection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentLinkedQueue

data class TrackedSignalEvent(
    val id: String,
    val symbol: String,
    val timeframe: String,
    val timestamp: Long,
    val direction: SignalDirection,
    val entryPrice: Double,
    val targetPrice: Double,
    val stopPrice: Double,
    val horizonBars: Int,
    val forecastProbability: Double,
    val isEvaluated: Boolean = false,
    val isWin: Boolean = false,
    val exitPrice: Double? = null,
    val exitReason: String? = null,
    val realizedPnlPercent: Double? = null
)

data class SignalTrackingMetrics(
    val totalSignalsLogged: Int = 0,
    val evaluatedSignalsCount: Int = 0,
    val targetHitCount: Int = 0,
    val stopHitCount: Int = 0,
    val expiredCount: Int = 0,
    val empiricalWinRatePercent: Double = 0.0,
    val meanForecastProbability: Double = 0.0,
    val averageRealizedReturnPercent: Double = 0.0,
    val calibrationError: Double = 0.0 // abs(empiricalWinRate - meanForecast)
)

/**
 * Signal-tracking harness that audits signals produced on closed candles
 * and verifies realized outcomes against forecasted probabilities.
 */
class SignalTrackingHarness {

    private val signalQueue = ConcurrentLinkedQueue<TrackedSignalEvent>()
    private val _metrics = MutableStateFlow(SignalTrackingMetrics())
    val metrics: StateFlow<SignalTrackingMetrics> = _metrics.asStateFlow()

    fun logSignal(
        signal: MarketSignal,
        timeframe: String,
        entryPrice: Double
    ) {
        val forecast = signal.forecast ?: return
        val id = "${signal.symbol}_${signal.timestamp}"

        val tracked = TrackedSignalEvent(
            id = id,
            symbol = signal.symbol,
            timeframe = timeframe,
            timestamp = signal.timestamp,
            direction = signal.direction,
            entryPrice = entryPrice,
            targetPrice = forecast.targetPrice,
            stopPrice = forecast.stopLossPrice,
            horizonBars = forecast.horizonCandles,
            forecastProbability = if (signal.direction.isBullish) forecast.probabilityUp else forecast.probabilityDown
        )
        signalQueue.add(tracked)
        recalculateMetrics()
    }

    fun onNewClosedCandle(candle: Candle) {
        val updatedList = mutableListOf<TrackedSignalEvent>()
        for (event in signalQueue) {
            if (event.isEvaluated) {
                updatedList.add(event)
                continue
            }

            val isLong = event.direction.isBullish
            val targetHit = if (isLong) candle.high >= event.targetPrice else candle.low <= event.targetPrice
            val stopHit = if (isLong) candle.low <= event.stopPrice else candle.high >= event.stopPrice

            if (targetHit) {
                val pnl = if (isLong) ((event.targetPrice - event.entryPrice) / event.entryPrice) * 100.0
                else ((event.entryPrice - event.targetPrice) / event.entryPrice) * 100.0
                updatedList.add(
                    event.copy(
                        isEvaluated = true,
                        isWin = true,
                        exitPrice = event.targetPrice,
                        exitReason = "TARGET_HIT",
                        realizedPnlPercent = pnl
                    )
                )
            } else if (stopHit) {
                val pnl = if (isLong) ((event.stopPrice - event.entryPrice) / event.entryPrice) * 100.0
                else ((event.entryPrice - event.stopPrice) / event.entryPrice) * 100.0
                updatedList.add(
                    event.copy(
                        isEvaluated = true,
                        isWin = false,
                        exitPrice = event.stopPrice,
                        exitReason = "STOP_HIT",
                        realizedPnlPercent = pnl
                    )
                )
            } else {
                updatedList.add(event)
            }
        }

        signalQueue.clear()
        signalQueue.addAll(updatedList)
        recalculateMetrics()
    }

    private fun recalculateMetrics() {
        val all = signalQueue.toList()
        val evaluated = all.filter { it.isEvaluated }
        val wins = evaluated.filter { it.isWin }
        val targets = evaluated.count { it.exitReason == "TARGET_HIT" }
        val stops = evaluated.count { it.exitReason == "STOP_HIT" }
        val expired = evaluated.count { it.exitReason == "HORIZON_EXPIRED" }

        val winRate = if (evaluated.isNotEmpty()) (wins.size.toDouble() / evaluated.size) * 100.0 else 0.0
        val meanProb = if (all.isNotEmpty()) all.map { it.forecastProbability }.average() * 100.0 else 0.0
        val avgReturn = if (evaluated.isNotEmpty()) evaluated.mapNotNull { it.realizedPnlPercent }.average() else 0.0
        val calErr = kotlin.math.abs(winRate - meanProb)

        _metrics.value = SignalTrackingMetrics(
            totalSignalsLogged = all.size,
            evaluatedSignalsCount = evaluated.size,
            targetHitCount = targets,
            stopHitCount = stops,
            expiredCount = expired,
            empiricalWinRatePercent = winRate,
            meanForecastProbability = meanProb,
            averageRealizedReturnPercent = avgReturn,
            calibrationError = calErr
        )
    }

    fun getAllSignals(): List<TrackedSignalEvent> = signalQueue.toList()
}
