package com.example

import com.example.data.local.dao.NewsArticleDao
import com.example.data.local.db.NewsArticleEntity
import com.example.data.remote.news.CryptoCompareNewsItemDto
import com.example.data.remote.news.CryptoCompareNewsResponse
import com.example.data.remote.news.CryptoNewsApiService
import com.example.data.remote.news.RssFeedParser
import com.example.data.repository.NewsRepositoryImpl
import com.example.domain.model.NewsArticle
import com.example.domain.model.NewsSentiment
import com.example.domain.repository.NewsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class NewsRepositoryTest {

    @Test
    fun testSentimentDetection() {
        val bullText = "Bitcoin ETF inflows surge to all-time high as breakout continues"
        val bearText = "Crypto market crash: Major token plunges 20% amid fraud and SEC lawsuit"
        val neutralText = "Federal Reserve meets today to discuss economic metrics"

        assertEquals(NewsSentiment.BULLISH, NewsArticle.detectSentiment(bullText))
        assertEquals(NewsSentiment.BEARISH, NewsArticle.detectSentiment(bearText))
        assertEquals(NewsSentiment.NEUTRAL, NewsArticle.detectSentiment(neutralText))
    }

    @Test
    fun testSymbolExtraction() {
        val text = "BTC and ETH continue to gain momentum against SOL"
        val symbols = NewsArticle.extractSymbols(text)

        assertTrue(symbols.contains("BTC"))
        assertTrue(symbols.contains("ETH"))
        assertTrue(symbols.contains("SOL"))
    }

    @Test
    fun testRssFeedParsing() {
        val sampleRss = """
            <?xml version="1.0" encoding="UTF-8"?>
            <rss version="2.0">
                <channel>
                    <title>CoinDesk</title>
                    <item>
                        <title>Bitcoin Breaks Records with Massive Adoption Inflows</title>
                        <link>https://www.coindesk.com/markets/bitcoin-surge</link>
                        <pubDate>Mon, 06 Oct 2026 12:00:00 +0000</pubDate>
                        <description>Institutional buyers expanded holdings by 40,000 BTC.</description>
                    </item>
                </channel>
            </rss>
        """.trimIndent()

        val articles = RssFeedParser.parse(ByteArrayInputStream(sampleRss.toByteArray()), "CoinDesk")

        assertEquals(1, articles.size)
        val first = articles.first()
        assertEquals("Bitcoin Breaks Records with Massive Adoption Inflows", first.title)
        assertEquals("CoinDesk", first.source)
        assertEquals("https://www.coindesk.com/markets/bitcoin-surge", first.url)
        assertEquals(NewsSentiment.BULLISH, first.sentiment)
    }

    private class FakeNewsDao : NewsArticleDao {
        private val storage = mutableListOf<NewsArticleEntity>()

        override fun getAllNewsFlow(limit: Int): Flow<List<NewsArticleEntity>> = flowOf(storage.toList())

        override fun getNewsBySymbolFlow(symbol: String, limit: Int): Flow<List<NewsArticleEntity>> {
            return flowOf(storage.filter { it.relatedSymbols.contains(symbol) })
        }

        override suspend fun getRecentNews(limit: Int): List<NewsArticleEntity> = storage.toList()

        override suspend fun insertArticles(articles: List<NewsArticleEntity>) {
            articles.forEach { art ->
                storage.removeAll { it.id == art.id }
                storage.add(art)
            }
        }

        override suspend fun deleteOldArticles(cutoffTime: Long): Int = 0

        override suspend fun clearAll() { storage.clear() }

        override suspend fun getCount(): Int = storage.size
    }

    private class FakeCryptoNewsApi : CryptoNewsApiService {
        override suspend fun getNews(lang: String, categories: String?): CryptoCompareNewsResponse {
            return CryptoCompareNewsResponse(
                data = listOf(
                    CryptoCompareNewsItemDto(
                        id = "api-1",
                        publishedOn = 1700000000L,
                        title = "Solana DEX Ecosystem Reaches Record Trading Highs",
                        url = "https://example.com/solana",
                        source = "CryptoCompare",
                        body = "Volume expanded as adoption soared."
                    )
                )
            )
        }
    }

    @Test
    fun testNewsRepositoryFlowAndRefresh() = runBlocking {
        val fakeDao = FakeNewsDao()
        val repository: NewsRepository = NewsRepositoryImpl(
            apiService = FakeCryptoNewsApi(),
            newsDao = fakeDao,
            okHttpClient = OkHttpClient()
        )

        repository.refreshNews()
        val list = repository.getNewsFlow().first()

        assertEquals(1, list.size)
        val item = list.first()
        assertEquals("Solana DEX Ecosystem Reaches Record Trading Highs", item.title)
        assertEquals(NewsSentiment.BULLISH, item.sentiment)
    }
}
