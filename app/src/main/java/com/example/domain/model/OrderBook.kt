package com.example.domain.model

data class OrderBookEntry(
    val price: Double,
    val quantity: Double
) {
    val total: Double get() = price * quantity
}

data class OrderBook(
    val symbol: String,
    val bids: List<OrderBookEntry>,
    val asks: List<OrderBookEntry>,
    val timestamp: Long = System.currentTimeMillis()
) {
    val bestBid: Double? get() = bids.firstOrNull()?.price
    val bestAsk: Double? get() = asks.firstOrNull()?.price
    val spread: Double get() = if (bestAsk != null && bestBid != null) bestAsk!! - bestBid!! else 0.0
    val spreadPercent: Double get() = if (bestBid != null && bestBid!! > 0) (spread / bestBid!!) * 100.0 else 0.0

    val totalBidVolume: Double get() = bids.sumOf { it.quantity }
    val totalAskVolume: Double get() = asks.sumOf { it.quantity }
    val bidAskRatio: Double get() = if (totalAskVolume > 0) totalBidVolume / totalAskVolume else 1.0
}
