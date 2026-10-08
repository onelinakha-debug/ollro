package com.example.data.remote.news

import com.example.domain.model.NewsArticle
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.UUID

object RssFeedParser {

    private val rfc822DateFormat = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US)

    fun parse(inputStream: InputStream, sourceName: String = "CryptoFeed"): List<NewsArticle> {
        val articles = mutableListOf<NewsArticle>()
        try {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = false
            val parser = factory.newPullParser()
            parser.setInput(inputStream, "UTF-8")

            var eventType = parser.eventType
            var currentTitle = ""
            var currentLink = ""
            var currentPubDate = ""
            var currentDescription = ""
            var currentImageUrl: String? = null
            var insideItem = false

            while (eventType != XmlPullParser.END_DOCUMENT) {
                val tagName = parser.name?.lowercase(Locale.US) ?: ""

                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        if (tagName == "item") {
                            insideItem = true
                            currentTitle = ""
                            currentLink = ""
                            currentPubDate = ""
                            currentDescription = ""
                            currentImageUrl = null
                        } else if (insideItem) {
                            when (tagName) {
                                "title" -> currentTitle = parser.nextText()
                                "link" -> currentLink = parser.nextText()
                                "pubdate" -> currentPubDate = parser.nextText()
                                "description" -> currentDescription = cleanHtml(parser.nextText())
                                "enclosure", "media:content" -> {
                                    val type = parser.getAttributeValue(null, "type")
                                    if (type?.startsWith("image") == true) {
                                        currentImageUrl = parser.getAttributeValue(null, "url")
                                    }
                                }
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (tagName == "item") {
                            insideItem = false
                            if (currentTitle.isNotBlank()) {
                                val timestamp = parseDate(currentPubDate)
                                val combinedText = "$currentTitle $currentDescription"
                                val sentiment = NewsArticle.detectSentiment(combinedText)
                                val symbols = NewsArticle.extractSymbols(combinedText)

                                articles.add(
                                    NewsArticle(
                                        id = currentLink.ifBlank { UUID.randomUUID().toString() },
                                        title = currentTitle.trim(),
                                        source = sourceName,
                                        url = currentLink.trim(),
                                        imageUrl = currentImageUrl,
                                        publishedAt = timestamp,
                                        body = currentDescription.take(300),
                                        categories = symbols.joinToString(","),
                                        sentiment = sentiment,
                                        relatedSymbols = symbols
                                    )
                                )
                            }
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (ignored: Exception) {
            // Ignore XML parse errors gracefully
        }
        return articles
    }

    private fun cleanHtml(html: String): String {
        return html.replace(Regex("<.*?>"), "").trim()
    }

    private fun parseDate(dateStr: String): Long {
        if (dateStr.isBlank()) return System.currentTimeMillis()
        return try {
            rfc822DateFormat.parse(dateStr)?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }
}
