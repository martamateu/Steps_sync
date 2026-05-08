package com.stepssync

import android.app.Application
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Application entry point.
 *
 * Schedules [SyncWorker] as a periodic WorkManager job the first time the app
 * launches (and after device reboots – WorkManager reschedules jobs
 * automatically).  Using [ExistingPeriodicWorkPolicy.KEEP] means a second call
 * to [schedulePeriodicSync] (e.g. after an upgrade) does NOT reset the timer.
 */
class MainApplication : Application() {

    companion object {
        private const val TAG = "MainApplication"
    }

    override fun onCreate() {
        super.onCreate()
        schedulePeriodicSync()
    }

    private fun schedulePeriodicSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest =
            PeriodicWorkRequestBuilder<SyncWorker>(
                Constants.SYNC_INTERVAL_HOURS, TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .addTag(Constants.SYNC_WORK_NAME)
                .build()

        WorkManager.getInstance(this)
            .enqueueUniquePeriodicWork(
                Constants.SYNC_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                syncRequest
            )

        Log.i(TAG, "Periodic sync scheduled (interval=${Constants.SYNC_INTERVAL_HOURS}h)")
    }
}
