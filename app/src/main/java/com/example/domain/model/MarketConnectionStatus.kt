package com.example.domain.model

sealed class MarketConnectionStatus {
    object Disconnected : MarketConnectionStatus()
    object Connecting : MarketConnectionStatus()
    data class Connected(val latencyMs: Long = 42L) : MarketConnectionStatus()
    data class FallbackLive(val reason: String = "HTTP 451 (Georestricted IP)") : MarketConnectionStatus()
    data class Reconnecting(val attempt: Int, val delaySeconds: Long) : MarketConnectionStatus()
    data class Stale(val elapsedSeconds: Long) : MarketConnectionStatus()

    val isOnline: Boolean get() = this is Connected || this is FallbackLive
    val isStale: Boolean get() = this is Stale
}
