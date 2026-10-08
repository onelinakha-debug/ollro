package com.example.data.remote.binance

import retrofit2.http.GET
import retrofit2.http.Query

interface BinanceApiService {

    @GET("api/v3/klines")
    suspend fun getKlines(
        @Query("symbol") symbol: String,
        @Query("interval") interval: String = "1m",
        @Query("limit") limit: Int = 100
    ): List<List<Any>>

    @GET("api/v3/ticker/24hr")
    suspend fun getTicker24h(
        @Query("symbol") symbol: String
    ): BinanceTickerResponse

    @GET("api/v3/ticker/24hr")
    suspend fun getAllTickers24h(): List<BinanceTickerResponse>

    @GET("api/v3/depth")
    suspend fun getOrderBook(
        @Query("symbol") symbol: String,
        @Query("limit") limit: Int = 20
    ): BinanceDepthResponse
}

data class BinanceTickerResponse(
    val symbol: String,
    val priceChange: String,
    val priceChangePercent: String,
    val lastPrice: String,
    val highPrice: String,
    val lowPrice: String,
    val volume: String,
    val quoteVolume: String
)

data class BinanceDepthResponse(
    val lastUpdateId: Long,
    val bids: List<List<String>>,
    val asks: List<List<String>>
)
