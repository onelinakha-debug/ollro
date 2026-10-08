package com.example

import android.app.Application
import android.util.Log
import androidx.work.Configuration
import com.example.core.notification.NotificationHelper
import com.example.di.OlroContainer

class OlroApplication : Application(), Configuration.Provider {

    lateinit var container: OlroContainer
        private set

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(Log.INFO)
            .build()

    override fun onCreate() {
        super.onCreate()
        container = OlroContainer(this)
        NotificationHelper.createNotificationChannel(this)
        try {
            container.alertManager.startBackgroundPolling(15)
        } catch (e: Throwable) {
            Log.w("OlroApplication", "WorkManager deferred start: ${e.message}")
        }
    }
}
