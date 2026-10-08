package com.example.domain.model

data class BinaryOptionAsset(
    val id: String,
    val name: String,
    val symbol: String,
    val payoutPercent: Int, // e.g. 92%
    val isOtc: Boolean = true,
    val currentPrice: Double,
    val priceChange24h: Double = 0.0,
    val signal: SignalDirection = SignalDirection.NEUTRAL
)
