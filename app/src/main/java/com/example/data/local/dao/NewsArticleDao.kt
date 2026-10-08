package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.db.NewsArticleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NewsArticleDao {

    @Query("SELECT * FROM news_articles ORDER BY publishedAt DESC LIMIT :limit")
    fun getAllNewsFlow(limit: Int = 50): Flow<List<NewsArticleEntity>>

    @Query("SELECT * FROM news_articles WHERE relatedSymbols LIKE '%' || :symbol || '%' ORDER BY publishedAt DESC LIMIT :limit")
    fun getNewsBySymbolFlow(symbol: String, limit: Int = 50): Flow<List<NewsArticleEntity>>

    @Query("SELECT * FROM news_articles ORDER BY publishedAt DESC LIMIT :limit")
    suspend fun getRecentNews(limit: Int = 50): List<NewsArticleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArticles(articles: List<NewsArticleEntity>)

    @Query("DELETE FROM news_articles WHERE publishedAt < :cutoffTime")
    suspend fun deleteOldArticles(cutoffTime: Long): Int

    @Query("DELETE FROM news_articles")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM news_articles")
    suspend fun getCount(): Int
}
