package com.example.presentation.journal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.remote.gemini.GeminiTradeExplainer
import com.example.domain.model.JournalPortfolioSummary
import com.example.domain.model.MarketSignal
import com.example.domain.model.SignalDirection
import com.example.domain.model.TradeJournalEntry
import com.example.domain.repository.JournalRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class JournalFilter {
    ALL,
    OPEN,
    WINNERS,
    LOSERS
}

class JournalViewModel(
    private val journalRepository: JournalRepository,
    private val geminiTradeExplainer: GeminiTradeExplainer
) : ViewModel() {

    private val _selectedFilter = MutableStateFlow(JournalFilter.ALL)
    val selectedFilter: StateFlow<JournalFilter> = _selectedFilter.asStateFlow()

    private val _explainingTradeId = MutableStateFlow<Long?>(null)
    val explainingTradeId: StateFlow<Long?> = _explainingTradeId.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    val summary: StateFlow<JournalPortfolioSummary> = journalRepository.summaryFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = JournalPortfolioSummary()
        )

    val filteredEntries: StateFlow<List<TradeJournalEntry>> = combine(
        journalRepository.allEntriesFlow,
        _selectedFilter
    ) { entries, filter ->
        when (filter) {
            JournalFilter.ALL -> entries
            JournalFilter.OPEN -> entries.filter { it.isOpen }
            JournalFilter.WINNERS -> entries.filter { it.isClosed && it.isWin }
            JournalFilter.LOSERS -> entries.filter { it.isClosed && !it.isWin }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun setFilter(filter: JournalFilter) {
        _selectedFilter.value = filter
    }

    fun logTrade(
        symbol: String,
        timeframe: String,
        direction: SignalDirection,
        entryPrice: Double,
        sizeUsd: Double,
        targetPrice: Double?,
        stopLossPrice: Double?,
        notes: String = "",
        forecastProbability: Double? = null
    ) {
        viewModelScope.launch {
            val entry = TradeJournalEntry(
                symbol = symbol.uppercase(),
                timeframe = timeframe,
                direction = direction,
                entryPrice = entryPrice,
                sizeUsd = sizeUsd,
                targetPrice = targetPrice,
                stopLossPrice = stopLossPrice,
                notes = notes,
                forecastProbability = forecastProbability,
                status = "OPEN"
            )
            journalRepository.logTrade(entry)
            _statusMessage.value = "Trade logged: ${direction.name} $symbol at $entryPrice"
        }
    }

    fun logTradeFromSignal(
        signal: MarketSignal,
        timeframe: String,
        currentPrice: Double,
        sizeUsd: Double = 500.0
    ) {
        viewModelScope.launch {
            val forecast = signal.forecast
            val entry = TradeJournalEntry(
                symbol = signal.symbol,
                timeframe = timeframe,
                direction = signal.direction,
                entryPrice = currentPrice,
                sizeUsd = sizeUsd,
                targetPrice = forecast?.targetPrice,
                stopLossPrice = forecast?.stopLossPrice,
                forecastProbability = if (signal.direction.isBullish) forecast?.probabilityUp else forecast?.probabilityDown,
                notes = "Auto-logged from ${signal.direction.name} (Forecast: ${forecast?.formatForecastStatement() ?: "N/A"})",
                status = "OPEN"
            )
            journalRepository.logTrade(entry)
            _statusMessage.value = "Signal recorded to Journal: ${signal.symbol} ${signal.direction.name}"
        }
    }

    fun closeTrade(id: Long, exitPrice: Double) {
        viewModelScope.launch {
            val closed = journalRepository.closeTrade(id, exitPrice)
            if (closed != null) {
                _statusMessage.value = "Trade closed with P&L: %s$%,.2f (%+.2f%%)".format(
                    java.util.Locale.US,
                    if ((closed.pnlUsd ?: 0.0) >= 0) "+" else "",
                    closed.pnlUsd ?: 0.0,
                    closed.pnlPercent ?: 0.0
                )
            }
        }
    }

    fun explainTradeWithGemini(trade: TradeJournalEntry) {
        viewModelScope.launch {
            _explainingTradeId.value = trade.id
            val analysis = geminiTradeExplainer.explainTrade(trade)
            journalRepository.updateAiAnalysis(trade.id, analysis)
            _explainingTradeId.value = null
        }
    }

    fun deleteTrade(id: Long) {
        viewModelScope.launch {
            journalRepository.deleteEntry(id)
            _statusMessage.value = "Trade removed from journal."
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }
}
