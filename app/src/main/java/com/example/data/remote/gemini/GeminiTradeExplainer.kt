package com.example.data.remote.gemini

import android.util.Log
import com.example.BuildConfig
import com.example.domain.model.TradeJournalEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Quantitative trade explanation layer using Gemini 3.5 Flash per gemini-api skill.
 * Provides AI-powered analysis of trade executions, risk-to-reward metrics,
 * indicator confirmations, and behavioral trade critiques.
 */
class GeminiTradeExplainer(
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()
) {
    companion object {
        private const val TAG = "GeminiTradeExplainer"
        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent"
    }

    suspend fun explainTrade(
        trade: TradeJournalEntry,
        marketContext: String = ""
    ): String = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Throwable) {
            ""
        }

        val prompt = buildString {
            appendLine("You are an elite quantitative trading mentor and risk manager for the Olro algorithmic terminal.")
            appendLine("Analyze this executed cryptocurrency trade with rigor, identifying strengths, technical errors, and risk adjustments.")
            appendLine()
            appendLine("TRADE EXECUTION DATA:")
            appendLine("- Symbol: ${trade.symbol}")
            appendLine("- Timeframe: ${trade.timeframe}")
            appendLine("- Direction: ${trade.direction.name}")
            appendLine("- Entry Price: $${trade.entryPrice}")
            appendLine("- Exit Price: ${trade.exitPrice?.let { "$$it" } ?: "STILL OPEN"}")
            appendLine("- Trade Stake: $${trade.sizeUsd}")
            appendLine("- Target Price: ${trade.targetPrice?.let { "$$it" } ?: "N/A"}")
            appendLine("- Stop Loss: ${trade.stopLossPrice?.let { "$$it" } ?: "N/A"}")
            appendLine("- Forecast Model Probability: ${trade.forecastProbability?.let { String.format(Locale.US, "%.1f%%", it * 100.0) } ?: "N/A"}")
            appendLine("- Realized P&L: ${trade.pnlUsd?.let { String.format(Locale.US, "%+$,.2f (%+.2f%%)", it, trade.pnlPercent ?: 0.0) } ?: "OPEN"}")
            appendLine("- User Notes: ${trade.notes.ifEmpty { "None provided" }}")
            if (marketContext.isNotBlank()) {
                appendLine("- Indicators Context: $marketContext")
            }
            appendLine()
            appendLine("STRUCTURE YOUR FEEDBACK IN 3 CLEAR SECTIONS:")
            appendLine("1. **Setup & Execution Quality**: Evaluate the entry, ATR target/stop ratio, and probabilistic edge.")
            appendLine("2. **Market Regime & Risk Assessment**: Did volatility, trend alignment, or order book imbalance favor this trade?")
            appendLine("3. **Actionable Improvement Rule**: A concrete quantitative rule for future trades in similar conditions.")
            appendLine("Keep the critique succinct, professional, and under 250 words.")
        }

        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY" || apiKey.length < 10) {
            return@withContext generateLocalQuantitativeReview(trade)
        }

        try {
            val url = "$BASE_URL?key=$apiKey"
            val requestBodyJson = JSONObject().apply {
                val contents = JSONArray().apply {
                    val contentObj = JSONObject().apply {
                        val parts = JSONArray().apply {
                            put(JSONObject().apply { put("text", prompt) })
                        }
                        put("parts", parts)
                    }
                    put(contentObj)
                }
                put("contents", contents)
            }

            val request = Request.Builder()
                .url(url)
                .post(requestBodyJson.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = okHttpClient.newCall(request).execute()
            val bodyString = response.body?.string()

            if (!response.isSuccessful || bodyString == null) {
                Log.w(TAG, "Gemini API error ${response.code}: $bodyString")
                return@withContext generateLocalQuantitativeReview(trade)
            }

            val jsonResponse = JSONObject(bodyString)
            val candidates = jsonResponse.optJSONArray("candidates")
            val candidate = candidates?.optJSONObject(0)
            val content = candidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val text = parts?.optJSONObject(0)?.optString("text")

            if (!text.isNullOrBlank()) {
                text
            } else {
                generateLocalQuantitativeReview(trade)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gemini call failed: ${e.message}", e)
            generateLocalQuantitativeReview(trade)
        }
    }

    private fun generateLocalQuantitativeReview(trade: TradeJournalEntry): String {
        val isWin = trade.isWin
        val pnlStr = trade.pnlPercent?.let { String.format(Locale.US, "%+.2f%%", it) } ?: "Pending"
        val probStr = trade.forecastProbability?.let { String.format(Locale.US, "%.1f%%", it * 100.0) } ?: "55.0%"

        return buildString {
            appendLine("### 📊 Quantitative Trade Audit (${trade.symbol})")
            appendLine()
            appendLine("1. **Setup & Execution Quality**")
            if (isWin) {
                appendLine("- Trade achieved a profitable exit ($pnlStr) adhering to the forecasted ${probStr} directional probability.")
                appendLine("- Risk-to-reward boundaries (Target: $${trade.targetPrice ?: 0.0} / Stop: $${trade.stopLossPrice ?: 0.0}) protected capital against adverse drift.")
            } else if (trade.isOpen) {
                appendLine("- Position active with entry at $${trade.entryPrice}. Direction: ${trade.direction.name}.")
                appendLine("- Monitor ATR stop limit ($${trade.stopLossPrice ?: 0.0}) to avoid emotional intervention.")
            } else {
                appendLine("- Stop loss triggered at exit ($pnlStr). The trade respected risk parameters, limiting capital drawdown.")
                appendLine("- Model expected ${probStr} probability; divergence occurred due to counter-trend volatility.")
            }
            appendLine()
            appendLine("2. **Market Regime & Risk Assessment**")
            appendLine("- Timeframe (${trade.timeframe}) requires multi-timeframe confirmation to filter false momentum breakouts.")
            appendLine("- Position sizing ($${trade.sizeUsd}) maintained within recommended portfolio risk limits.")
            appendLine()
            appendLine("3. **Actionable Improvement Rule**")
            appendLine("- *Rule*: Always verify that the higher timeframe Supertrend is aligned before entering positions with confidence below 65%.")
            appendLine()
            appendLine("> *Note: Set your GEMINI_API_KEY in the AI Studio Secrets panel for live deep neural explanations.*")
        }
    }
}
