package com.example.data.local.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.domain.model.Candle
import com.example.domain.model.TechnicalIndicators

@Entity(
    tableName = "candles",
    indices = [
        Index(value = ["symbol", "interval", "timestamp"], unique = true),
        Index(value = ["symbol", "interval"]),
        Index(value = ["timestamp"])
    ]
)
data class CandleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val symbol: String,
    val interval: String,
    val timestamp: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Double,
    val closeTime: Long,
    val isClosed: Boolean,
    val indicators: TechnicalIndicators? = null
) {
    fun toDomain(): Candle = Candle(
        timestamp = timestamp,
        open = open,
        high = high,
        low = low,
        close = close,
        volume = volume,
        closeTime = closeTime,
        isClosed = isClosed
    )

    companion object {
        fun fromDomain(
            symbol: String,
            interval: String,
            candle: Candle,
            indicators: TechnicalIndicators? = null
        ): CandleEntity = CandleEntity(
            symbol = symbol,
            interval = interval,
            timestamp = candle.timestamp,
            open = candle.open,
            high = candle.high,
            low = candle.low,
            close = candle.close,
            volume = candle.volume,
            closeTime = candle.closeTime,
            isClosed = candle.isClosed,
            indicators = indicators
        )
    }
}
