package com.example.domain.model

enum class NewsSentiment {
    BULLISH,
    BEARISH,
    NEUTRAL
}

data class NewsArticle(
    val id: String,
    val title: String,
    val source: String,
    val url: String,
    val imageUrl: String? = null,
    val publishedAt: Long = System.currentTimeMillis(),
    val body: String = "",
    val categories: String = "",
    val sentiment: NewsSentiment = NewsSentiment.NEUTRAL,
    val relatedSymbols: List<String> = emptyList()
) {
    companion object {
        fun detectSentiment(text: String): NewsSentiment {
            val lower = text.lowercase()
            val bullishKeywords = listOf(
                "surge", "rally", "bull", "all-time high", "ath", "breakout", "gain", "soar",
                "adoption", "approved", "etf", "inflow", "jump", "record", "pump", "upgrade"
            )
            val bearishKeywords = listOf(
                "crash", "drop", "plunge", "bear", "sell-off", "hack", "ban", "lawsuit", "sec",
                "fraud", "dump", "decline", "outflow", "liquidation", "fall", "warning", "risk"
            )

            val bullCount = bullishKeywords.count { lower.contains(it) }
            val bearCount = bearishKeywords.count { lower.contains(it) }

            return when {
                bullCount > bearCount -> NewsSentiment.BULLISH
                bearCount > bullCount -> NewsSentiment.BEARISH
                else -> NewsSentiment.NEUTRAL
            }
        }

        fun extractSymbols(text: String): List<String> {
            val symbols = mutableListOf<String>()
            val candidates = listOf("BTC", "ETH", "SOL", "BNB", "DOGE", "XRP", "ADA", "AVAX")
            val upper = text.uppercase()
            for (sym in candidates) {
                if (upper.contains(sym)) symbols.add(sym)
            }
            return if (symbols.isEmpty()) listOf("CRYPTO") else symbols.distinct()
        }
    }
}
