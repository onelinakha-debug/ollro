package com.example.core.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

class OkHttpClientProvider(
    private val enablePinning: Boolean = false
) {
    val restClient: OkHttpClient by lazy {
        buildClient(pingIntervalSeconds = 0)
    }

    val webSocketClient: OkHttpClient by lazy {
        // Binance requires regular pings to keep the connection alive
        buildClient(pingIntervalSeconds = 25)
    }

    private fun buildClient(pingIntervalSeconds: Long): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        val builder = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .addInterceptor(loggingInterceptor)

        if (pingIntervalSeconds > 0) {
            builder.pingInterval(pingIntervalSeconds, TimeUnit.SECONDS)
        }

        if (enablePinning) {
            builder.certificatePinner(CertificatePinningConfig.certificatePinner)
        }

        return builder.build()
    }
}
