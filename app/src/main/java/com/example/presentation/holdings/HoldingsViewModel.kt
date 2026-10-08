package com.example.presentation.holdings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.manager.MarketDataManager
import com.example.domain.model.PortfolioSummary
import com.example.domain.repository.HoldingRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HoldingsViewModel(
    private val holdingRepository: HoldingRepository,
    private val marketDataManager: MarketDataManager
) : ViewModel() {

    // Derived price feed from live marketDataManager streams
    private val livePriceFeed = combine(
        marketDataManager.topTickers,
        marketDataManager.currentTicker
    ) { topList, current ->
        val map = mutableMapOf<String, Double>()
        topList.forEach { map[it.symbol] = it.lastPrice }
        current?.let { map[it.symbol] = it.lastPrice }
        map
    }

    val portfolioSummary: StateFlow<PortfolioSummary> = holdingRepository
        .observePortfolioSummary(livePriceFeed)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PortfolioSummary())

    fun saveHolding(symbol: String, amount: Double, averageBuyPrice: Double) {
        viewModelScope.launch {
            holdingRepository.saveHolding(symbol, amount, averageBuyPrice)
        }
    }

    fun deleteHolding(id: Long) {
        viewModelScope.launch {
            holdingRepository.deleteHolding(id)
        }
    }
}
