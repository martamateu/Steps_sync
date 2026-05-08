package com.stepssync.data

import android.content.Context
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.stepssync.config.Constants
import java.time.LocalDate
import java.time.ZoneId

class HealthConnectRepository(context: Context) {

    private val appContext = context.applicationContext
    private val zoneId: ZoneId = ZoneId.systemDefault()
    private val client: HealthConnectClient by lazy {
        HealthConnectClient.getOrCreate(appContext)
    }

    val requiredPermissions: Set<String> = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class)
    )

    fun sdkStatus(): Int =
        HealthConnectClient.getSdkStatus(appContext, Constants.HEALTH_CONNECT_PACKAGE)

    fun isAvailable(): Boolean = sdkStatus() == HealthConnectClient.SDK_AVAILABLE

    /** Returns the last fully completed local day, which is the day this app syncs. */
    fun previousDayDate(): LocalDate = LocalDate.now(zoneId).minusDays(1)

    suspend fun hasPermissions(): Boolean {
        if (!isAvailable()) {
            Log.w(TAG, "Health Connect SDK not available (status=${sdkStatus()})")
            return false
        }
        val grantedPermissions = client.permissionController.getGrantedPermissions()
        val hasAll = grantedPermissions.containsAll(requiredPermissions)
        Log.i(TAG, "Permissions check – required=$requiredPermissions granted=$grantedPermissions hasAll=$hasAll")
        return hasAll
    }

    suspend fun getPreviousDaySteps(): Long {
        val syncDate = previousDayDate()
        val startTime = syncDate.atStartOfDay(zoneId).toInstant()
        val endTime = syncDate.plusDays(1).atStartOfDay(zoneId).toInstant()

        Log.d(TAG, "Querying steps for $syncDate ($startTime → $endTime) zone=$zoneId")

        val response = client.aggregate(
            AggregateRequest(
                metrics = setOf(StepsRecord.COUNT_TOTAL),
                timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
            )
        )

        val steps = response[StepsRecord.COUNT_TOTAL] ?: 0L
        Log.i(TAG, "Health Connect returned $steps steps for $syncDate")
        return steps
    }

    suspend fun getPreviousDayCalories(steps: Long): Double {
        val syncDate = previousDayDate()
        val startTime = syncDate.atStartOfDay(zoneId).toInstant()
        val endTime = syncDate.plusDays(1).atStartOfDay(zoneId).toInstant()
        val timeRange = TimeRangeFilter.between(startTime, endTime)

        // 1. Try active calories
        val activeResponse = client.aggregate(
            AggregateRequest(setOf(ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL), timeRange)
        )
        val active = activeResponse[ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL]?.inKilocalories ?: 0.0
        if (active > 0.0) {
            Log.i(TAG, "Using active calories: $active kcal for $syncDate")
            return active
        }

        // 2. Fallback to total calories
        val totalResponse = client.aggregate(
            AggregateRequest(setOf(TotalCaloriesBurnedRecord.ENERGY_TOTAL), timeRange)
        )
        val total = totalResponse[TotalCaloriesBurnedRecord.ENERGY_TOTAL]?.inKilocalories ?: 0.0
        if (total > 0.0) {
            Log.i(TAG, "Using total calories: $total kcal for $syncDate")
            return total
        }

        // 3. Estimate from steps (~0.04 kcal/step)
        val estimated = steps * 0.04
        Log.w(TAG, "No calorie data in Health Connect, estimating $estimated kcal from $steps steps")
        return estimated
    }

    /**
     * Compatibility entry point kept to match the requested project contract.
     *
     * It delegates to [getPreviousDaySteps] so the app always syncs the last fully completed day.
     */
    suspend fun getTodaySteps(): Long = getPreviousDaySteps()

    companion object {
        private const val TAG = "HealthConnectRepository"
    }
}
