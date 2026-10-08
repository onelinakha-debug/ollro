package com.example.data.remote.news

import retrofit2.http.GET
import retrofit2.http.Query

interface CryptoNewsApiService {

    @GET("data/v2/news/")
    suspend fun getNews(
        @Query("lang") lang: String = "EN",
        @Query("categories") categories: String? = null
    ): CryptoCompareNewsResponse
}
