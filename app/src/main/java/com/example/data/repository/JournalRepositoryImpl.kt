package com.example.data.repository

import com.example.data.local.dao.JournalDao
import com.example.data.local.db.JournalEntryEntity
import com.example.domain.model.JournalPortfolioSummary
import com.example.domain.model.SignalDirection
import com.example.domain.model.TradeJournalEntry
import com.example.domain.repository.JournalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.math.abs

class JournalRepositoryImpl(
    private val journalDao: JournalDao
) : JournalRepository {

    override val allEntriesFlow: Flow<List<TradeJournalEntry>> =
        journalDao.getAllEntriesFlow().map { list -> list.map { it.toDomain() } }

    override val openEntriesFlow: Flow<List<TradeJournalEntry>> =
        journalDao.getOpenEntriesFlow().map { list -> list.map { it.toDomain() } }

    override val closedEntriesFlow: Flow<List<TradeJournalEntry>> =
        journalDao.getClosedEntriesFlow().map { list -> list.map { it.toDomain() } }

    override val summaryFlow: Flow<JournalPortfolioSummary> =
        allEntriesFlow.map { entries ->
            val openCount = entries.count { it.isOpen }
            val closedEntries = entries.filter { it.isClosed }
            val closedCount = closedEntries.size
            val wins = closedEntries.filter { it.isWin }
            val losses = closedEntries.filter { !it.isWin }
            val winCount = wins.size
            val lossCount = losses.size

            val winRate = if (closedCount > 0) (winCount.toDouble() / closedCount) * 100.0 else 0.0
            val totalPnlUsd = closedEntries.sumOf { it.pnlUsd ?: 0.0 }
            val avgPnlPct = if (closedCount > 0) closedEntries.mapNotNull { it.pnlPercent }.average() else 0.0

            val grossProfit = wins.sumOf { it.pnlUsd ?: 0.0 }
            val grossLoss = losses.sumOf { abs(it.pnlUsd ?: 0.0) }
            val profitFactor = if (grossLoss > 0) grossProfit / grossLoss else if (grossProfit > 0) 99.0 else 1.0

            val avgWinUsd = if (wins.isNotEmpty()) wins.mapNotNull { it.pnlUsd }.average() else 0.0
            val avgLossUsd = if (losses.isNotEmpty()) losses.mapNotNull { it.pnlUsd?.let { p -> abs(p) } }.average() else 0.0
            val winRateDec = winRate / 100.0
            val lossRateDec = 1.0 - winRateDec
            val expectancy = (winRateDec * avgWinUsd) - (lossRateDec * avgLossUsd)

            JournalPortfolioSummary(
                totalTrades = entries.size,
                openTradesCount = openCount,
                closedTradesCount = closedCount,
                winCount = winCount,
                lossCount = lossCount,
                winRatePercent = winRate,
                totalRealizedPnlUsd = totalPnlUsd,
                averageTradePnlPercent = avgPnlPct,
                expectancyUsd = expectancy,
                profitFactor = profitFactor
            )
        }

    override suspend fun getEntryById(id: Long): TradeJournalEntry? {
        return journalDao.getEntryById(id)?.toDomain()
    }

    override suspend fun logTrade(entry: TradeJournalEntry): Long {
        val entity = JournalEntryEntity.fromDomain(entry)
        return journalDao.insertEntry(entity)
    }

    override suspend fun closeTrade(
        id: Long,
        exitPrice: Double,
        exitTime: Long
    ): TradeJournalEntry? {
        val existing = journalDao.getEntryById(id) ?: return null
        val isLong = existing.direction == SignalDirection.BUY.name || existing.direction == SignalDirection.STRONG_BUY.name

        val pnlPercent = if (isLong) {
            ((exitPrice - existing.entryPrice) / existing.entryPrice) * 100.0
        } else {
            ((existing.entryPrice - exitPrice) / existing.entryPrice) * 100.0
        }
        val pnlUsd = existing.sizeUsd * (pnlPercent / 100.0)

        val updated = existing.copy(
            status = "CLOSED",
            exitPrice = exitPrice,
            exitTimestamp = exitTime,
            pnlPercent = pnlPercent,
            pnlUsd = pnlUsd
        )
        journalDao.updateEntry(updated)
        return updated.toDomain()
    }

    override suspend fun updateAiAnalysis(id: Long, analysis: String) {
        val existing = journalDao.getEntryById(id) ?: return
        journalDao.updateEntry(existing.copy(aiAnalysis = analysis))
    }

    override suspend fun deleteEntry(id: Long) {
        journalDao.deleteEntryById(id)
    }

    override suspend fun clearAll() {
        journalDao.clearAll()
    }
}
