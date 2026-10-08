package com.example.data.remote.news

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CryptoCompareNewsResponse(
    @Json(name = "Data") val data: List<CryptoCompareNewsItemDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class CryptoCompareNewsItemDto(
    @Json(name = "id") val id: String,
    @Json(name = "guid") val guid: String? = null,
    @Json(name = "published_on") val publishedOn: Long,
    @Json(name = "imageurl") val imageUrl: String? = null,
    @Json(name = "title") val title: String,
    @Json(name = "url") val url: String,
    @Json(name = "source") val source: String? = null,
    @Json(name = "body") val body: String? = null,
    @Json(name = "tags") val tags: String? = null,
    @Json(name = "categories") val categories: String? = null
)
