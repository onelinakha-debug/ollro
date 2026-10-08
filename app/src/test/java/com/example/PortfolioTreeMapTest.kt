package com.example

import androidx.compose.ui.geometry.Rect
import com.example.domain.model.Holding
import com.example.domain.model.HoldingWithPnL
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PortfolioTreeMapTest {

    @Test
    fun testPortfolioWeightingCalculation() {
        val holdings = listOf(
            Holding(1, "BTCUSDT", 1.0, 90000.0).toHoldingWithPnL(100000.0), // $100,000 (66.67%)
            Holding(2, "ETHUSDT", 10.0, 3000.0).toHoldingWithPnL(3500.0),    // $35,000 (23.33%)
            Holding(3, "SOLUSDT", 50.0, 200.0).toHoldingWithPnL(300.0)       // $15,000 (10.0%)
        )

        val totalValue = holdings.sumOf { it.currentValue }
        assertEquals(150000.0, totalValue, 0.001)

        val btcWeight = (holdings[0].currentValue / totalValue) * 100.0
        val ethWeight = (holdings[1].currentValue / totalValue) * 100.0
        val solWeight = (holdings[2].currentValue / totalValue) * 100.0

        assertEquals(66.666, btcWeight, 0.01)
        assertEquals(23.333, ethWeight, 0.01)
        assertEquals(10.0, solWeight, 0.01)

        val totalWeight = btcWeight + ethWeight + solWeight
        assertEquals(100.0, totalWeight, 0.001)
    }

    @Test
    fun testSingleAssetTreeMapBounds() {
        val single = listOf(
            Holding(1, "BTCUSDT", 2.0, 90000.0).toHoldingWithPnL(95000.0)
        )

        val total = single.sumOf { it.currentValue }
        val weight = (single.first().currentValue / total) * 100.0
        assertEquals(100.0, weight, 0.001)
    }

    @Test
    fun testHoldingProfitColorClassification() {
        val profitHolding = Holding(1, "BTCUSDT", 1.0, 90000.0).toHoldingWithPnL(98000.0)
        val lossHolding = Holding(2, "ETHUSDT", 1.0, 3200.0).toHoldingWithPnL(2900.0)

        assertTrue(profitHolding.isProfit)
        assertTrue(profitHolding.pnl > 0)
        assertTrue(!lossHolding.isProfit)
        assertTrue(lossHolding.pnl < 0)
    }
}
