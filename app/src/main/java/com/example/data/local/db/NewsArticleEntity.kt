package com.example.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.domain.model.NewsArticle
import com.example.domain.model.NewsSentiment

@Entity(tableName = "news_articles")
data class NewsArticleEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val source: String,
    val url: String,
    val imageUrl: String?,
    val publishedAt: Long,
    val body: String,
    val categories: String,
    val sentiment: String,
    val relatedSymbols: String
) {
    fun toDomain(): NewsArticle = NewsArticle(
        id = id,
        title = title,
        source = source,
        url = url,
        imageUrl = imageUrl,
        publishedAt = publishedAt,
        body = body,
        categories = categories,
        sentiment = try { NewsSentiment.valueOf(sentiment) } catch (e: Exception) { NewsSentiment.NEUTRAL },
        relatedSymbols = relatedSymbols.split(",").filter { it.isNotBlank() }
    )

    companion object {
        fun fromDomain(article: NewsArticle): NewsArticleEntity = NewsArticleEntity(
            id = article.id,
            title = article.title,
            source = article.source,
            url = article.url,
            imageUrl = article.imageUrl,
            publishedAt = article.publishedAt,
            body = article.body,
            categories = article.categories,
            sentiment = article.sentiment.name,
            relatedSymbols = article.relatedSymbols.joinToString(",")
        )
    }
}
