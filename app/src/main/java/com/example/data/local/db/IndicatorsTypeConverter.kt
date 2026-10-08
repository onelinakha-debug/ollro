package com.example.data.local.db

import androidx.room.TypeConverter
import com.example.domain.model.BollingerBandsOutput
import com.example.domain.model.MacdOutput
import com.example.domain.model.SupertrendOutput
import com.example.domain.model.TechnicalIndicators
import org.json.JSONArray
import org.json.JSONObject

class IndicatorsTypeConverter {

    @TypeConverter
    fun fromTechnicalIndicators(indicators: TechnicalIndicators?): String? {
        if (indicators == null) return null
        return try {
            val root = JSONObject()
            indicators.rsi14?.let { root.put("rsi14", it) }
            indicators.ema9?.let { root.put("ema9", it) }
            indicators.ema21?.let { root.put("ema21", it) }
            indicators.ema50?.let { root.put("ema50", it) }
            indicators.ema200?.let { root.put("ema200", it) }
            indicators.atr14?.let { root.put("atr14", it) }
            indicators.vwap?.let { root.put("vwap", it) }
            indicators.rvol?.let { root.put("rvol", it) }

            indicators.macd?.let { macd ->
                val macdObj = JSONObject().apply {
                    put("macdLine", macd.macdLine)
                    put("signalLine", macd.signalLine)
                    put("histogram", macd.histogram)
                }
                root.put("macd", macdObj)
            }

            indicators.bollingerBands?.let { bb ->
                val bbObj = JSONObject().apply {
                    put("upper", bb.upper)
                    put("middle", bb.middle)
                    put("lower", bb.lower)
                    put("bandwidth", bb.bandwidth)
                    put("percentB", bb.percentB)
                }
                root.put("bollingerBands", bbObj)
            }

            indicators.supertrend?.let { st ->
                val stObj = JSONObject().apply {
                    put("value", st.value)
                    put("isBullish", st.isBullish)
                }
                root.put("supertrend", stObj)
            }

            root.toString()
        } catch (e: Exception) {
            null
        }
    }

    @TypeConverter
    fun toTechnicalIndicators(jsonStr: String?): TechnicalIndicators? {
        if (jsonStr.isNullOrBlank()) return null
        return try {
            val root = JSONObject(jsonStr)

            val rsi14 = if (root.has("rsi14")) root.getDouble("rsi14") else null
            val ema9 = if (root.has("ema9")) root.getDouble("ema9") else null
            val ema21 = if (root.has("ema21")) root.getDouble("ema21") else null
            val ema50 = if (root.has("ema50")) root.getDouble("ema50") else null
            val ema200 = if (root.has("ema200")) root.getDouble("ema200") else null
            val atr14 = if (root.has("atr14")) root.getDouble("atr14") else null
            val vwap = if (root.has("vwap")) root.getDouble("vwap") else null
            val rvol = if (root.has("rvol")) root.getDouble("rvol") else null

            val macd = if (root.has("macd")) {
                val m = root.getJSONObject("macd")
                MacdOutput(
                    macdLine = m.getDouble("macdLine"),
                    signalLine = m.getDouble("signalLine"),
                    histogram = m.getDouble("histogram")
                )
            } else null

            val bollingerBands = if (root.has("bollingerBands")) {
                val b = root.getJSONObject("bollingerBands")
                BollingerBandsOutput(
                    upper = b.getDouble("upper"),
                    middle = b.getDouble("middle"),
                    lower = b.getDouble("lower"),
                    bandwidth = b.getDouble("bandwidth"),
                    percentB = b.getDouble("percentB")
                )
            } else null

            val supertrend = if (root.has("supertrend")) {
                val s = root.getJSONObject("supertrend")
                SupertrendOutput(
                    value = s.getDouble("value"),
                    isBullish = s.getBoolean("isBullish")
                )
            } else null

            TechnicalIndicators(
                rsi14 = rsi14,
                macd = macd,
                bollingerBands = bollingerBands,
                ema9 = ema9,
                ema21 = ema21,
                ema50 = ema50,
                ema200 = ema200,
                atr14 = atr14,
                vwap = vwap,
                supertrend = supertrend,
                rvol = rvol
            )
        } catch (e: Exception) {
            null
        }
    }

    @TypeConverter
    fun fromMacdOutput(macd: MacdOutput?): String? {
        if (macd == null) return null
        return try {
            JSONObject().apply {
                put("macdLine", macd.macdLine)
                put("signalLine", macd.signalLine)
                put("histogram", macd.histogram)
            }.toString()
        } catch (e: Exception) {
            null
        }
    }

    @TypeConverter
    fun toMacdOutput(jsonStr: String?): MacdOutput? {
        if (jsonStr.isNullOrBlank()) return null
        return try {
            val root = JSONObject(jsonStr)
            MacdOutput(
                macdLine = root.getDouble("macdLine"),
                signalLine = root.getDouble("signalLine"),
                histogram = root.getDouble("histogram")
            )
        } catch (e: Exception) {
            null
        }
    }

    @TypeConverter
    fun fromBollingerBandsOutput(bb: BollingerBandsOutput?): String? {
        if (bb == null) return null
        return try {
            JSONObject().apply {
                put("upper", bb.upper)
                put("middle", bb.middle)
                put("lower", bb.lower)
                put("bandwidth", bb.bandwidth)
                put("percentB", bb.percentB)
            }.toString()
        } catch (e: Exception) {
            null
        }
    }

    @TypeConverter
    fun toBollingerBandsOutput(jsonStr: String?): BollingerBandsOutput? {
        if (jsonStr.isNullOrBlank()) return null
        return try {
            val root = JSONObject(jsonStr)
            BollingerBandsOutput(
                upper = root.getDouble("upper"),
                middle = root.getDouble("middle"),
                lower = root.getDouble("lower"),
                bandwidth = root.getDouble("bandwidth"),
                percentB = root.getDouble("percentB")
            )
        } catch (e: Exception) {
            null
        }
    }

    @TypeConverter
    fun fromSupertrendOutput(st: SupertrendOutput?): String? {
        if (st == null) return null
        return try {
            JSONObject().apply {
                put("value", st.value)
                put("isBullish", st.isBullish)
            }.toString()
        } catch (e: Exception) {
            null
        }
    }

    @TypeConverter
    fun toSupertrendOutput(jsonStr: String?): SupertrendOutput? {
        if (jsonStr.isNullOrBlank()) return null
        return try {
            val root = JSONObject(jsonStr)
            SupertrendOutput(
                value = root.getDouble("value"),
                isBullish = root.getBoolean("isBullish")
            )
        } catch (e: Exception) {
            null
        }
    }

    @TypeConverter
    fun fromDoubleList(list: List<Double>?): String? {
        if (list == null) return null
        val array = JSONArray()
        list.forEach { array.put(it) }
        return array.toString()
    }

    @TypeConverter
    fun toDoubleList(jsonStr: String?): List<Double>? {
        if (jsonStr.isNullOrBlank()) return null
        return try {
            val array = JSONArray(jsonStr)
            val result = mutableListOf<Double>()
            for (i in 0 until array.length()) {
                result.add(array.getDouble(i))
            }
            result
        } catch (e: Exception) {
            null
        }
    }
}
