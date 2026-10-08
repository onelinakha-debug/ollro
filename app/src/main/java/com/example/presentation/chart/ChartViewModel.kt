package com.example.presentation.chart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.indicators.IndicatorCalculator
import com.example.domain.manager.MarketDataManager
import com.example.domain.model.Candle
import com.example.domain.model.MacdOutput
import com.example.domain.model.MarketConnectionStatus
import com.example.domain.model.MarketSignal
import com.example.domain.model.TechnicalIndicators
import com.example.domain.model.Ticker24h
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class ChartViewModel(
    private val marketDataManager: MarketDataManager
) : ViewModel() {

    val selectedSymbol: StateFlow<String> = marketDataManager.selectedSymbol
    val selectedInterval: StateFlow<String> = marketDataManager.selectedInterval
    val candles: StateFlow<List<Candle>> = marketDataManager.candlesState
    val currentTicker: StateFlow<Ticker24h?> = marketDataManager.currentTicker
    val indicators: StateFlow<TechnicalIndicators> = marketDataManager.indicatorsState
    val signal: StateFlow<MarketSignal?> = marketDataManager.signalState
    val connectionStatus: StateFlow<MarketConnectionStatus> = marketDataManager.connectionStatus

    private val _showRsi = MutableStateFlow(true)
    val showRsi: StateFlow<Boolean> = _showRsi.asStateFlow()

    private val _showMacd = MutableStateFlow(true)
    val showMacd: StateFlow<Boolean> = _showMacd.asStateFlow()

    private val _hoveredCandle = MutableStateFlow<Candle?>(null)
    val hoveredCandle: StateFlow<Candle?> = _hoveredCandle.asStateFlow()

    val rsiHistory: StateFlow<List<Double>> = candles.map { list ->
        IndicatorCalculator.calculateRSI(list, 14)
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val macdHistory: StateFlow<List<MacdOutput>> = candles.map { list ->
        IndicatorCalculator.calculateMACD(list.map { it.close }, 12, 26, 9)
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun selectInterval(interval: String) {
        marketDataManager.selectInterval(interval)
    }

    fun selectSymbol(symbol: String) {
        marketDataManager.selectSymbolAndInterval(symbol)
    }

    fun toggleRsi() {
        _showRsi.value = !_showRsi.value
    }

    fun toggleMacd() {
        _showMacd.value = !_showMacd.value
    }

    fun setHoveredCandle(candle: Candle?) {
        _hoveredCandle.value = candle
    }
}
