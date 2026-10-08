package com.example.domain.model

data class Ticker24h(
    val symbol: String,
    val lastPrice: Double,
    val priceChange: Double,
    val priceChangePercent: Double,
    val highPrice24h: Double,
    val lowPrice24h: Double,
    val volume24h: Double,
    val quoteVolume24h: Double,
    val timestamp: Long = System.currentTimeMillis()
) {
    val isPositive: Boolean get() = priceChangePercent >= 0.0
}
