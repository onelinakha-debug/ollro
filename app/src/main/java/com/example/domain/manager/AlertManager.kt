package com.example.domain.manager

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.example.data.worker.AlertPollingWorker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit

class AlertManager(private val context: Context) {

    private val workManager: WorkManager?
        get() = try {
            WorkManager.getInstance(context)
        } catch (e: Throwable) {
            null
        }

    /**
     * Starts background polling using WorkManager to evaluate price thresholds
     * and indicator conditions even when the app is in the background or killed.
     */
    fun startBackgroundPolling(intervalMinutes: Long = 15) {
        val wm = workManager ?: return
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val periodicRequest = PeriodicWorkRequestBuilder<AlertPollingWorker>(
            repeatInterval = intervalMinutes.coerceAtLeast(15),
            repeatIntervalTimeUnit = TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .addTag(AlertPollingWorker.TAG)
            .build()

        wm.enqueueUniquePeriodicWork(
            AlertPollingWorker.WORK_NAME_PERIODIC,
            ExistingPeriodicWorkPolicy.KEEP,
            periodicRequest
        )
    }

    /**
     * Triggers an immediate one-time background surveillance check.
     */
    fun triggerImmediateCheck() {
        val wm = workManager ?: return
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val oneTimeRequest = OneTimeWorkRequestBuilder<AlertPollingWorker>()
            .setConstraints(constraints)
            .addTag(AlertPollingWorker.TAG)
            .build()

        wm.enqueueUniqueWork(
            AlertPollingWorker.WORK_NAME_ONE_TIME,
            ExistingWorkPolicy.REPLACE,
            oneTimeRequest
        )
    }

    /**
     * Stops background alert polling.
     */
    fun stopBackgroundPolling() {
        workManager?.cancelUniqueWork(AlertPollingWorker.WORK_NAME_PERIODIC)
    }

    /**
     * Observes whether the background polling worker is currently scheduled or running.
     */
    fun isPollingActiveFlow(): Flow<Boolean> {
        val wm = workManager ?: return flowOf(false)
        return wm.getWorkInfosForUniqueWorkFlow(AlertPollingWorker.WORK_NAME_PERIODIC).map { list ->
            list.any { it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.RUNNING }
        }
    }
}
