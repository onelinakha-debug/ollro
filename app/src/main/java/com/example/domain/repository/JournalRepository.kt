package com.example.domain.repository

import com.example.domain.model.JournalPortfolioSummary
import com.example.domain.model.TradeJournalEntry
import kotlinx.coroutines.flow.Flow

interface JournalRepository {
    val allEntriesFlow: Flow<List<TradeJournalEntry>>
    val openEntriesFlow: Flow<List<TradeJournalEntry>>
    val closedEntriesFlow: Flow<List<TradeJournalEntry>>
    val summaryFlow: Flow<JournalPortfolioSummary>

    suspend fun getEntryById(id: Long): TradeJournalEntry?
    suspend fun logTrade(entry: TradeJournalEntry): Long
    suspend fun closeTrade(id: Long, exitPrice: Double, exitTime: Long = System.currentTimeMillis()): TradeJournalEntry?
    suspend fun updateAiAnalysis(id: Long, analysis: String)
    suspend fun deleteEntry(id: Long)
    suspend fun clearAll()
}
