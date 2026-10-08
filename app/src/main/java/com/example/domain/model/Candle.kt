package com.example.domain.model

data class Candle(
    val timestamp: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Double,
    val closeTime: Long = timestamp + 60_000L,
    val isClosed: Boolean = true
) {
    val isBullish: Boolean get() = close >= open
    val bodySize: Double get() = Math.abs(close - open)
    val upperWick: Double get() = high - Math.max(open, close)
    val lowerWick: Double get() = Math.min(open, close) - low
    val changePercent: Double get() = if (open > 0.0) ((close - open) / open) * 100.0 else 0.0
}
