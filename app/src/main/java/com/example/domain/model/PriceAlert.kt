package com.example.domain.model

enum class AlertCondition {
    PRICE_ABOVE,
    PRICE_BELOW,
    RSI_OVERBOUGHT,
    RSI_OVERSOLD,
    MACD_BULLISH_CROSS,
    MACD_BEARISH_CROSS
}

data class PriceAlert(
    val id: Long = 0,
    val symbol: String,
    val condition: AlertCondition,
    val targetValue: Double,
    val isTriggered: Boolean = false,
    val isEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
