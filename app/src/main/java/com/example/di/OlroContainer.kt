package com.example.di

import android.content.Context
import com.example.core.network.OkHttpClientProvider
import com.example.core.security.KeyStoreManager
import com.example.data.local.db.OlroDatabase
import com.example.data.remote.binance.BinanceApiService
import com.example.data.remote.binance.BinanceWebSocketManager
import com.example.data.remote.pocketoption.PocketOptionClient
import com.example.data.remote.pocketoption.SessionManager
import com.example.data.remote.news.CryptoNewsApiService
import com.example.data.repository.AlertRepository
import com.example.data.repository.BinanceRepository
import com.example.data.repository.CandleRepositoryImpl
import com.example.data.repository.HoldingRepositoryImpl
import com.example.data.repository.NewsRepositoryImpl
import com.example.data.repository.PocketOptionRepository
import com.example.domain.manager.MarketDataManager
import com.example.domain.repository.CandleRepository
import com.example.domain.repository.HoldingRepository
import com.example.domain.repository.NewsRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

class OlroContainer(private val context: Context) {

    val keyStoreManager: KeyStoreManager by lazy {
        KeyStoreManager()
    }

    val okHttpClientProvider: OkHttpClientProvider by lazy {
        OkHttpClientProvider(enablePinning = false)
    }

    val database: OlroDatabase by lazy {
        OlroDatabase.getInstance(context)
    }

    private val moshi: Moshi by lazy {
        Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.binance.com/")
            .client(okHttpClientProvider.restClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    val binanceApiService: BinanceApiService by lazy {
        retrofit.create(BinanceApiService::class.java)
    }

    val binanceWebSocketManager: BinanceWebSocketManager by lazy {
        BinanceWebSocketManager(okHttpClientProvider.webSocketClient)
    }

    val binanceRepository: BinanceRepository by lazy {
        BinanceRepository(
            apiService = binanceApiService,
            candleDao = database.candleDao(),
            webSocketManager = binanceWebSocketManager
        )
    }

    val candleRepository: CandleRepository by lazy {
        CandleRepositoryImpl(
            candleDao = database.candleDao()
        )
    }

    val alertRepository: AlertRepository by lazy {
        AlertRepository(
            alertDao = database.alertDao()
        )
    }

    val holdingRepository: HoldingRepository by lazy {
        HoldingRepositoryImpl(
            holdingDao = database.holdingDao()
        )
    }

    private val cryptoNewsRetrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("https://min-api.cryptocompare.com/")
            .client(okHttpClientProvider.restClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    val cryptoNewsApiService: CryptoNewsApiService by lazy {
        cryptoNewsRetrofit.create(CryptoNewsApiService::class.java)
    }

    val newsRepository: NewsRepository by lazy {
        NewsRepositoryImpl(
            apiService = cryptoNewsApiService,
            newsDao = database.newsArticleDao(),
            okHttpClient = okHttpClientProvider.restClient
        )
    }

    val alertManager: com.example.domain.manager.AlertManager by lazy {
        com.example.domain.manager.AlertManager(context)
    }

    val sessionManager: SessionManager by lazy {
        SessionManager(
            context = context,
            keyStoreManager = keyStoreManager
        )
    }

    val pocketOptionClient: PocketOptionClient by lazy {
        PocketOptionClient(
            okHttpClient = okHttpClientProvider.webSocketClient,
            sessionManager = sessionManager
        )
    }

    val pocketOptionRepository: PocketOptionRepository by lazy {
        PocketOptionRepository(
            client = pocketOptionClient,
            sessionManager = sessionManager
        )
    }

    val marketDataManager: MarketDataManager by lazy {
        MarketDataManager.getInstance(
            binanceRepository = binanceRepository,
            alertRepository = alertRepository
        )
    }
}
