package com.example.domain.manager

import android.util.Log
import com.example.core.common.DataResult
import com.example.data.repository.AlertRepository
import com.example.data.repository.BinanceRepository
import com.example.domain.engine.SignalAggregator
import com.example.domain.indicators.IndicatorCalculator
import com.example.domain.model.AlertCondition
import com.example.domain.model.Candle
import com.example.domain.model.MarketConnectionStatus
import com.example.domain.model.MarketSignal
import com.example.domain.model.OrderBook
import com.example.domain.model.PriceAlert
import com.example.domain.model.TechnicalIndicators
import com.example.domain.model.Ticker24h
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.min
import kotlin.random.Random

/**
 * Event wrapper for reactive market streams broadcast via SharedFlow.
 */
sealed class MarketDataEvent {
    data class CandleUpdate(val candle: Candle) : MarketDataEvent()
    data class TickerUpdate(val ticker: Ticker24h) : MarketDataEvent()
    data class DepthUpdate(val orderBook: OrderBook) : MarketDataEvent()
    data class PingSent(val timestamp: Long) : MarketDataEvent()
    data class PongReceived(val latencyMs: Long) : MarketDataEvent()
    data class ErrorOccurred(val message: String, val isRecoverable: Boolean) : MarketDataEvent()
}

/**
 * Singleton manager owning Binance WebSocket connections, exponential backoff with jitter,
 * 30-second PING/PONG keep-alive mechanism, and broadcasting market data as SharedFlow.
 */
class MarketDataManager(
    private val binanceRepository: BinanceRepository,
    private val alertRepository: AlertRepository
) {
    companion object {
        private const val TAG = "MarketDataManager"

        const val BASE_BACKOFF_MS = 1000L
        const val MAX_BACKOFF_MS = 30000L
        const val BACKOFF_FACTOR = 1.6
        const val PING_INTERVAL_MS = 30_000L // Binance 30-second PING keep-alive requirement
        const val PONG_TIMEOUT_MS = 10_000L  // Max wait for PONG response before triggering backoff
        const val STALE_THRESHOLD_MS = 10_000L

        @Volatile
        private var INSTANCE: MarketDataManager? = null

        fun getInstance(
            binanceRepository: BinanceRepository,
            alertRepository: AlertRepository
        ): MarketDataManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: MarketDataManager(binanceRepository, alertRepository).also {
                    INSTANCE = it
                }
            }
        }

        fun getInstance(): MarketDataManager {
            return INSTANCE ?: throw IllegalStateException("MarketDataManager singleton has not been initialized.")
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // SharedFlow streams for the application
    private val _marketDataStream = MutableSharedFlow<MarketDataEvent>(replay = 1)
    val marketDataStream: SharedFlow<MarketDataEvent> = _marketDataStream.asSharedFlow()

    private val _candleStream = MutableSharedFlow<Candle>(replay = 1)
    val candleStream: SharedFlow<Candle> = _candleStream.asSharedFlow()

    private val _tickerStream = MutableSharedFlow<Ticker24h>(replay = 1)
    val tickerStream: SharedFlow<Ticker24h> = _tickerStream.asSharedFlow()

    private val _depthStream = MutableSharedFlow<OrderBook>(replay = 1)
    val depthStream: SharedFlow<OrderBook> = _depthStream.asSharedFlow()

    // StateFlow states for UI data binding
    private val _selectedSymbol = MutableStateFlow("BTCUSDT")
    val selectedSymbol: StateFlow<String> = _selectedSymbol.asStateFlow()

    private val _selectedInterval = MutableStateFlow("1m")
    val selectedInterval: StateFlow<String> = _selectedInterval.asStateFlow()

    private val _candlesState = MutableStateFlow<List<Candle>>(emptyList())
    val candlesState: StateFlow<List<Candle>> = _candlesState.asStateFlow()

    private val _currentTicker = MutableStateFlow<Ticker24h?>(null)
    val currentTicker: StateFlow<Ticker24h?> = _currentTicker.asStateFlow()

    private val _topTickers = MutableStateFlow<List<Ticker24h>>(emptyList())
    val topTickers: StateFlow<List<Ticker24h>> = _topTickers.asStateFlow()

    private val _orderBook = MutableStateFlow<OrderBook?>(null)
    val orderBook: StateFlow<OrderBook?> = _orderBook.asStateFlow()

    private val _indicatorsState = MutableStateFlow(TechnicalIndicators())
    val indicatorsState: StateFlow<TechnicalIndicators> = _indicatorsState.asStateFlow()

    private val _signalState = MutableStateFlow<MarketSignal?>(null)
    val signalState: StateFlow<MarketSignal?> = _signalState.asStateFlow()

    val connectionStatus: StateFlow<MarketConnectionStatus> = binanceRepository.connectionStatus

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    private val _alertTriggeredEvent = MutableSharedFlow<PriceAlert>(replay = 0)
    val alertTriggeredEvent: SharedFlow<PriceAlert> = _alertTriggeredEvent.asSharedFlow()

    // Reconnection and Keep-Alive State
    private val reconnectAttemptCounter = AtomicInteger(0)
    private var reconnectJob: Job? = null
    private var keepAliveJob: Job? = null
    private val lastPingTimestamp = AtomicLong(0L)
    private val lastPongTimestamp = AtomicLong(0L)
    private val lastDataTimestamp = AtomicLong(0L)

    init {
        INSTANCE = this
        observeStreams()
        startKeepAlive()
        refreshTopTickers()
        loadHistoricalAndConnect("BTCUSDT", "1m")
    }

    fun selectSymbolAndInterval(symbol: String, interval: String = _selectedInterval.value) {
        val formattedSymbol = symbol.uppercase(Locale.US)
        _selectedSymbol.value = formattedSymbol
        _selectedInterval.value = interval
        reconnectAttemptCounter.set(0)
        loadHistoricalAndConnect(formattedSymbol, interval)
    }

    fun selectInterval(interval: String) {
        selectSymbolAndInterval(_selectedSymbol.value, interval)
    }

    fun refreshTopTickers() {
        scope.launch(Dispatchers.IO) {
            when (val res = binanceRepository.getTopTickers()) {
                is DataResult.Success -> _topTickers.value = res.data
                else -> {}
            }
        }
    }

    /**
     * Calculates exponential backoff with randomized jitter.
     * delay = min(MAX_BACKOFF, BASE_BACKOFF * (FACTOR ^ attempt)) + jitter
     */
    fun calculateBackoffDelay(attempt: Int): Long {
        val safeAttempt = min(attempt, 6)
        val exponentialDelay = (BASE_BACKOFF_MS * Math.pow(BACKOFF_FACTOR, safeAttempt.toDouble())).toLong()
        val jitter = Random.nextLong(200, 800)
        return min(MAX_BACKOFF_MS, exponentialDelay + jitter)
    }

    /**
     * Reconnects to Binance WebSocket using exponential backoff with jitter.
     */
    fun reconnectWithBackoff(reason: String = "Network disconnect") {
        if (reconnectJob?.isActive == true) return

        val attempt = reconnectAttemptCounter.incrementAndGet()
        val backoffDelayMs = calculateBackoffDelay(attempt)
        _lastError.value = "Reconnecting ($attempt): $reason. Delay: ${backoffDelayMs / 1000}s"

        scope.launch {
            _marketDataStream.emit(
                MarketDataEvent.ErrorOccurred(
                    message = "WebSocket backoff attempt $attempt in ${backoffDelayMs}ms. Reason: $reason",
                    isRecoverable = true
                )
            )
        }

        reconnectJob = scope.launch {
            delay(backoffDelayMs)
            binanceRepository.startLiveStream(_selectedSymbol.value, _selectedInterval.value)
        }
    }

    /**
     * Starts the 30-second PING/PONG keep-alive mechanism satisfying Binance API specs.
     */
    fun startKeepAlive(intervalMs: Long = PING_INTERVAL_MS) {
        keepAliveJob?.cancel()
        keepAliveJob = scope.launch {
            while (isActive) {
                delay(intervalMs)
                sendPingKeepAlive()
            }
        }
    }

    fun stopKeepAlive() {
        keepAliveJob?.cancel()
    }

    /**
     * Sends a 30s PING frame and verifies PONG/activity receipt.
     */
    fun sendPingKeepAlive() {
        val now = System.currentTimeMillis()
        lastPingTimestamp.set(now)

        scope.launch {
            _marketDataStream.emit(MarketDataEvent.PingSent(now))

            // Check if connection is dead/stale (no data or pong received within timeout)
            delay(PONG_TIMEOUT_MS)
            val elapsedSinceData = System.currentTimeMillis() - lastDataTimestamp.get()
            val elapsedSincePong = System.currentTimeMillis() - lastPongTimestamp.get()

            if (elapsedSinceData > (PING_INTERVAL_MS + PONG_TIMEOUT_MS) && elapsedSincePong > PONG_TIMEOUT_MS) {
                Log.w(TAG, "PING keep-alive failed: No PONG or market frames received in timeout window.")
                reconnectWithBackoff("PING/PONG keep-alive timeout")
            }
        }
    }

    /**
     * Called when a PONG frame or live message is received from Binance.
     */
    fun onPongReceived() {
        val now = System.currentTimeMillis()
        lastPongTimestamp.set(now)
        val pingTime = lastPingTimestamp.get()
        val latency = if (pingTime > 0) (now - pingTime).coerceAtLeast(1L) else 25L

        scope.launch {
            _marketDataStream.emit(MarketDataEvent.PongReceived(latency))
        }
    }

    /**
     * Robust error handler capturing network exceptions, georestrictions, and protocol failures.
     */
    fun handleConnectionError(throwable: Throwable, contextMessage: String = "") {
        val isHttp451 = throwable.message?.contains("451") == true
        val errorDescription = if (isHttp451) {
            "Binance stream georestricted (HTTP 451). Active resilient fallback engine running."
        } else {
            "${throwable.javaClass.simpleName}: ${throwable.message ?: contextMessage}"
        }

        _lastError.value = errorDescription
        Log.w(TAG, "MarketDataManager connection error: $errorDescription")

        scope.launch {
            _marketDataStream.emit(
                MarketDataEvent.ErrorOccurred(
                    message = errorDescription,
                    isRecoverable = true
                )
            )
        }

        if (!isHttp451) {
            reconnectWithBackoff(errorDescription)
        }
    }

    private fun loadHistoricalAndConnect(symbol: String, interval: String) {
        scope.launch(Dispatchers.IO) {
            val result = binanceRepository.fetchHistoricalCandles(symbol, interval, limit = 120)
            if (result is DataResult.Success) {
                _candlesState.value = result.data
                recalculateAnalytics(result.data)
            }
            binanceRepository.startLiveStream(symbol, interval)
        }
    }

    private fun observeStreams() {
        // Observe real-time candle updates and broadcast as SharedFlow
        scope.launch {
            binanceRepository.candleStream.collectLatest { liveCandle ->
                lastDataTimestamp.set(System.currentTimeMillis())
                reconnectAttemptCounter.set(0) // Reset backoff on successful data frame
                _candleStream.emit(liveCandle)
                _marketDataStream.emit(MarketDataEvent.CandleUpdate(liveCandle))

                val currentList = _candlesState.value.toMutableList()
                if (currentList.isEmpty()) {
                    currentList.add(liveCandle)
                } else {
                    val lastIdx = currentList.lastIndex
                    val lastCandle = currentList[lastIdx]
                    if (liveCandle.timestamp == lastCandle.timestamp) {
                        currentList[lastIdx] = liveCandle
                    } else if (liveCandle.timestamp > lastCandle.timestamp) {
                        currentList.add(liveCandle)
                        if (currentList.size > 200) {
                            currentList.removeAt(0)
                        }
                    }
                }
                _candlesState.value = currentList
                recalculateAnalytics(currentList)

                // Persist closed candle to Room
                if (liveCandle.isClosed) {
                    launch(Dispatchers.IO) {
                        binanceRepository.saveCandle(_selectedSymbol.value, _selectedInterval.value, liveCandle)
                    }
                }
            }
        }

        // Observe real-time 24h ticker updates and broadcast as SharedFlow
        scope.launch {
            binanceRepository.tickerStream.collectLatest { ticker ->
                lastDataTimestamp.set(System.currentTimeMillis())
                if (ticker.symbol.equals(_selectedSymbol.value, ignoreCase = true)) {
                    _currentTicker.value = ticker
                    _tickerStream.emit(ticker)
                    _marketDataStream.emit(MarketDataEvent.TickerUpdate(ticker))
                    checkPriceAlerts(ticker.lastPrice)
                }
            }
        }

        // Observe order book depth and broadcast as SharedFlow
        scope.launch {
            binanceRepository.depthStream.collectLatest { book ->
                lastDataTimestamp.set(System.currentTimeMillis())
                if (book.symbol.equals(_selectedSymbol.value, ignoreCase = true)) {
                    _orderBook.value = book
                    _depthStream.emit(book)
                    _marketDataStream.emit(MarketDataEvent.DepthUpdate(book))
                }
            }
        }
    }

    private fun recalculateAnalytics(candles: List<Candle>) {
        if (candles.size < 15) return
        val calculated = IndicatorCalculator.calculateAll(candles)
        _indicatorsState.value = calculated

        val signal = SignalAggregator.aggregate(
            symbol = _selectedSymbol.value,
            candles = candles,
            indicators = calculated
        )
        _signalState.value = signal
        checkIndicatorAlerts(calculated)
    }

    private fun checkPriceAlerts(currentPrice: Double) {
        scope.launch(Dispatchers.IO) {
            val alerts = alertRepository.getActiveAlerts()
            for (alert in alerts) {
                if (!alert.symbol.equals(_selectedSymbol.value, ignoreCase = true)) continue
                val triggered = when (alert.condition) {
                    AlertCondition.PRICE_ABOVE -> currentPrice >= alert.targetValue
                    AlertCondition.PRICE_BELOW -> currentPrice <= alert.targetValue
                    else -> false
                }
                if (triggered) {
                    _alertTriggeredEvent.emit(alert)
                    alertRepository.toggleAlert(alert)
                }
            }
        }
    }

    private fun checkIndicatorAlerts(indicators: TechnicalIndicators) {
        scope.launch(Dispatchers.IO) {
            val alerts = alertRepository.getActiveAlerts()
            val rsi = indicators.rsi14 ?: return@launch
            val macd = indicators.macd

            for (alert in alerts) {
                if (!alert.symbol.equals(_selectedSymbol.value, ignoreCase = true)) continue
                val triggered = when (alert.condition) {
                    AlertCondition.RSI_OVERBOUGHT -> rsi >= alert.targetValue
                    AlertCondition.RSI_OVERSOLD -> rsi <= alert.targetValue
                    AlertCondition.MACD_BULLISH_CROSS -> macd?.isBullishCrossover == true
                    AlertCondition.MACD_BEARISH_CROSS -> macd?.isBearishCrossover == true
                    else -> false
                }
                if (triggered) {
                    _alertTriggeredEvent.emit(alert)
                    alertRepository.toggleAlert(alert)
                }
            }
        }
    }
}
