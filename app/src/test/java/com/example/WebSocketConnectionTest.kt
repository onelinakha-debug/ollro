package com.example

import com.example.data.remote.binance.BinanceWebSocketManager
import com.example.domain.model.MarketConnectionStatus
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WebSocketConnectionTest {

    @Test
    fun testExponentialBackoffCalculations() {
        val manager = BinanceWebSocketManager(OkHttpClient())

        val delay1 = manager.calculateBackoffDelayForTest(1)
        val delay2 = manager.calculateBackoffDelayForTest(2)
        val delay3 = manager.calculateBackoffDelayForTest(3)
        val delay4 = manager.calculateBackoffDelayForTest(4)
        val delayLarge = manager.calculateBackoffDelayForTest(20)

        assertTrue("Delay 2 must exceed Delay 1", delay2 > delay1)
        assertTrue("Delay 3 must exceed Delay 2", delay3 > delay2)
        assertTrue("Delay 4 must exceed Delay 3", delay4 > delay3)

        // Verify base is around 1600ms for attempt 1
        assertTrue(delay1 in 1500..1700)
    }

    @Test
    fun testConnectionStatusTransitions() {
        val connected = MarketConnectionStatus.Connected(45L)
        assertTrue(connected.isOnline)

        val reconnecting = MarketConnectionStatus.Reconnecting(attempt = 2, delaySeconds = 4L)
        assertEquals(2, reconnecting.attempt)
        assertEquals(4L, reconnecting.delaySeconds)

        val stale = MarketConnectionStatus.Stale(elapsedSeconds = 12L)
        assertTrue(stale.isStale)
        assertEquals(12L, stale.elapsedSeconds)
    }
}
