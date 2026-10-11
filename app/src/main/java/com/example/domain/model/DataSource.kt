package com.example.domain.model

/**
 * Where a set of candles came from. Analytics must never treat [SIMULATED] data as a real market.
 */
enum class DataSource {
    /** Fetched from the exchange just now. */
    LIVE,

    /** Exchange unreachable; served from the local Room cache of earlier real candles. */
    CACHED,

    /** Randomly generated placeholder candles. Not market data; signals must be disabled. */
    SIMULATED
}
