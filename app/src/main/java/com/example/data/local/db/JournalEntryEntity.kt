package com.example.data.local.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.domain.model.SignalDirection
import com.example.domain.model.TradeJournalEntry

@Entity(
    tableName = "trade_journal",
    indices = [
        Index(value = ["symbol"]),
        Index(value = ["status"]),
        Index(value = ["entryTimestamp"])
    ]
)
data class JournalEntryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val symbol: String,
    val timeframe: String,
    val direction: String,
    val entryPrice: Double,
    val exitPrice: Double?,
    val sizeUsd: Double,
    val targetPrice: Double?,
    val stopLossPrice: Double?,
    val pnlUsd: Double?,
    val pnlPercent: Double?,
    val status: String,
    val entryTimestamp: Long,
    val exitTimestamp: Long?,
    val forecastProbability: Double?,
    val notes: String,
    val aiAnalysis: String?,
    val strategyTag: String
) {
    fun toDomain(): TradeJournalEntry = TradeJournalEntry(
        id = id,
        symbol = symbol,
        timeframe = timeframe,
        direction = try { SignalDirection.valueOf(direction) } catch (e: Exception) { SignalDirection.BUY },
        entryPrice = entryPrice,
        exitPrice = exitPrice,
        sizeUsd = sizeUsd,
        targetPrice = targetPrice,
        stopLossPrice = stopLossPrice,
        pnlUsd = pnlUsd,
        pnlPercent = pnlPercent,
        status = status,
        entryTimestamp = entryTimestamp,
        exitTimestamp = exitTimestamp,
        forecastProbability = forecastProbability,
        notes = notes,
        aiAnalysis = aiAnalysis,
        strategyTag = strategyTag
    )

    companion object {
        fun fromDomain(entry: TradeJournalEntry): JournalEntryEntity = JournalEntryEntity(
            id = entry.id,
            symbol = entry.symbol,
            timeframe = entry.timeframe,
            direction = entry.direction.name,
            entryPrice = entry.entryPrice,
            exitPrice = entry.exitPrice,
            sizeUsd = entry.sizeUsd,
            targetPrice = entry.targetPrice,
            stopLossPrice = entry.stopLossPrice,
            pnlUsd = entry.pnlUsd,
            pnlPercent = entry.pnlPercent,
            status = entry.status,
            entryTimestamp = entry.entryTimestamp,
            exitTimestamp = entry.exitTimestamp,
            forecastProbability = entry.forecastProbability,
            notes = entry.notes,
            aiAnalysis = entry.aiAnalysis,
            strategyTag = entry.strategyTag
        )
    }
}
