package com.stepssync.sync

import android.content.Context
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.stepssync.data.HealthConnectRepository
import com.stepssync.data.SyncStateStore
import com.stepssync.network.ApiClient
import java.io.IOException
import java.time.LocalDate

class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val repository = HealthConnectRepository(appContext)
    private val stateStore = SyncStateStore(appContext)
    private val apiClient = ApiClient()

    override suspend fun doWork(): Result {
        val syncDate = repository.previousDayDate()
        Log.i(TAG, "Starting sync for $syncDate")

        return try {
            when (repository.sdkStatus()) {
                HealthConnectClient.SDK_UNAVAILABLE,
                HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> {
                    Log.w(TAG, "Health Connect unavailable for sync")
                    Result.retry()
                }
                else -> runSync(syncDate)
            }
        } catch (error: IOException) {
            Log.e(TAG, "Network error during sync", error)
            Result.retry()
        } catch (error: Exception) {
            Log.e(TAG, "Unexpected sync error", error)
            Result.retry()
        }
    }

    private suspend fun runSync(syncDate: LocalDate): Result {
        if (!repository.hasPermissions()) {
            Log.w(TAG, "Missing Health Connect permissions; sync aborted")
            return Result.failure()
        }

        if (stateStore.wasDateSynced(syncDate)) {
            Log.i(TAG, "Skipping duplicate sync for $syncDate")
            return Result.success()
        }

        val steps = repository.getPreviousDaySteps()
        apiClient.postSteps(syncDate, steps)
        stateStore.markDateSynced(syncDate, steps)

        Log.i(TAG, "Sync completed for $syncDate with $steps steps")
        return Result.success()
    }

    companion object {
        private const val TAG = "SyncWorker"
    }
}
