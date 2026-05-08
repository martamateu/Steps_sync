package com.stepssync

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import java.time.LocalDate

/**
 * Background worker that reads today's steps from Health Connect and posts
 * them to the configured webhook.
 *
 * Scheduled by [MainApplication] as a [androidx.work.PeriodicWorkRequest]
 * that repeats every 24 hours.  WorkManager ensures the job survives device
 * reboots and process death.
 *
 * Retry policy:
 * - Returns [Result.retry()] on transient failures (network, Health Connect
 *   unavailable) so WorkManager re-attempts with exponential back-off.
 * - Returns [Result.failure()] when permissions are not granted (no point in
 *   retrying until the user grants them).
 */
class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "SyncWorker"
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "doWork() started")

        val repository = HealthConnectRepository(applicationContext)

        if (!repository.hasPermissions()) {
            Log.w(TAG, "Health Connect permissions not granted – skipping sync")
            // Return failure so WorkManager does not retry until next scheduled run
            return Result.failure()
        }

        return try {
            val steps = repository.readTodaySteps()
            val date = LocalDate.now().toString()          // e.g. "2024-06-15"

            Log.i(TAG, "Syncing: date=$date steps=$steps")

            ApiClient.postSteps(date, steps)

            Log.i(TAG, "Sync successful – date=$date steps=$steps")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Sync failed – will retry", e)
            Result.retry()
        }
    }
}
