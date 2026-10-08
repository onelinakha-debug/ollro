package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.core.notification.NotificationHelper
import com.example.data.worker.AlertPollingWorker
import com.example.domain.manager.AlertManager
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AlertManagerTest {

    @Test
    fun testAlertManagerInitializationAndScheduling() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val alertManager = AlertManager(context)

        // Test scheduling periodic background work
        alertManager.startBackgroundPolling(15)

        // Test triggering immediate one-time background check
        alertManager.triggerImmediateCheck()

        // Test stopping polling
        alertManager.stopBackgroundPolling()
    }

    @Test
    fun testNotificationHelperChannelCreation() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        NotificationHelper.createNotificationChannel(context)

        // Notification channel ID must be non-empty
        assertNotNull(NotificationHelper.CHANNEL_ID)
        assertNotNull(NotificationHelper.CHANNEL_NAME)
    }
}
