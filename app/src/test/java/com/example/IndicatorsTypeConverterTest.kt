package com.example

import com.example.data.local.db.IndicatorsTypeConverter
import com.example.domain.model.BollingerBandsOutput
import com.example.domain.model.MacdOutput
import com.example.domain.model.SupertrendOutput
import com.example.domain.model.TechnicalIndicators
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class IndicatorsTypeConverterTest {

    private val converter = IndicatorsTypeConverter()

    @Test
    fun testTechnicalIndicatorsSerializationAndDeserialization() {
        val original = TechnicalIndicators(
            rsi14 = 68.45,
            macd = MacdOutput(macdLine = 124.5, signalLine = 110.2, histogram = 14.3),
            bollingerBands = BollingerBandsOutput(
                upper = 98500.0,
                middle = 96000.0,
                lower = 93500.0,
                bandwidth = 0.052,
                percentB = 0.82
            ),
            ema9 = 96400.0,
            ema21 = 95800.0,
            ema50 = 94200.0,
            ema200 = 89000.0,
            atr14 = 1450.25,
            vwap = 96150.0,
            supertrend = SupertrendOutput(value = 94800.0, isBullish = true),
            rvol = 1.45
        )

        val json = converter.fromTechnicalIndicators(original)
        assertNotNull("JSON output should not be null", json)
        assertTrue("JSON should contain rsi14", json!!.contains("68.45"))
        assertTrue("JSON should contain macd", json.contains("macdLine"))

        val restored = converter.toTechnicalIndicators(json)
        assertNotNull("Restored object should not be null", restored)
        assertEquals(68.45, restored!!.rsi14!!, 0.001)
        assertEquals(124.5, restored.macd!!.macdLine, 0.001)
        assertEquals(98500.0, restored.bollingerBands!!.upper, 0.001)
        assertEquals(true, restored.supertrend!!.isBullish)
        assertEquals(1.45, restored.rvol!!, 0.001)
    }

    @Test
    fun testNullHandling() {
        assertNull(converter.fromTechnicalIndicators(null))
        assertNull(converter.toTechnicalIndicators(null))
        assertNull(converter.toTechnicalIndicators(""))
    }

    @Test
    fun testMacdOutputConverter() {
        val macd = MacdOutput(macdLine = 15.2, signalLine = 12.0, histogram = 3.2)
        val json = converter.fromMacdOutput(macd)
        val restored = converter.toMacdOutput(json)

        assertNotNull(restored)
        assertEquals(15.2, restored!!.macdLine, 0.001)
        assertEquals(3.2, restored.histogram, 0.001)
    }

    @Test
    fun testBollingerBandsConverter() {
        val bb = BollingerBandsOutput(100.0, 95.0, 90.0, 0.1, 0.5)
        val json = converter.fromBollingerBandsOutput(bb)
        val restored = converter.toBollingerBandsOutput(json)

        assertNotNull(restored)
        assertEquals(100.0, restored!!.upper, 0.001)
        assertEquals(0.5, restored.percentB, 0.001)
    }
}
