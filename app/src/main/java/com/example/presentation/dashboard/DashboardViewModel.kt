package com.example.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.manager.MarketDataManager
import com.example.domain.model.MarketConnectionStatus
import com.example.domain.model.MarketSignal
import com.example.domain.model.OrderBook
import com.example.domain.model.TechnicalIndicators
import com.example.domain.model.Ticker24h
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DashboardViewModel(
    private val marketDataManager: MarketDataManager
) : ViewModel() {

    val selectedSymbol: StateFlow<String> = marketDataManager.selectedSymbol
    val currentTicker: StateFlow<Ticker24h?> = marketDataManager.currentTicker
    val topTickers: StateFlow<List<Ticker24h>> = marketDataManager.topTickers
    val orderBook: StateFlow<OrderBook?> = marketDataManager.orderBook
    val indicators: StateFlow<TechnicalIndicators> = marketDataManager.indicatorsState
    val signal: StateFlow<MarketSignal?> = marketDataManager.signalState
    val signalUnavailableReason: StateFlow<String?> = marketDataManager.signalUnavailableReason
    val connectionStatus: StateFlow<MarketConnectionStatus> = marketDataManager.connectionStatus

    fun selectSymbol(symbol: String) {
        marketDataManager.selectSymbolAndInterval(symbol)
    }

    fun refreshTickers() {
        marketDataManager.refreshTopTickers()
    }
}
