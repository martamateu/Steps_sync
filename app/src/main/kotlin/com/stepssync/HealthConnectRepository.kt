package com.stepssync

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.LocalDate
import java.time.ZoneId

/**
 * Provides read access to Health Connect step data.
 *
 * All public functions are `suspend` and must be called from a coroutine.
 * The [HealthConnectClient] is created lazily so construction is fast.
 */
class HealthConnectRepository(private val context: Context) {

    private val client: HealthConnectClient by lazy {
        HealthConnectClient.getOrCreate(context)
    }

    /** The set of Health Connect permissions this app requires. */
    val requiredPermissions: Set<String> = setOf(
        HealthPermission.getReadPermission(StepsRecord::class)
    )

    /**
     * Returns `true` if all [requiredPermissions] have already been granted
     * by the user.
     */
    suspend fun hasPermissions(): Boolean {
        val granted = client.permissionController.getGrantedPermissions()
        return granted.containsAll(requiredPermissions)
    }

    /**
     * Reads and aggregates the total step count for *today* (midnight-to-midnight
     * in the device's local time zone).
     *
     * @return Total steps recorded today, or 0 if no data is available.
     */
    suspend fun readTodaySteps(): Long {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val startOfDay = today.atStartOfDay(zone).toInstant()
        val endOfDay = today.plusDays(1).atStartOfDay(zone).toInstant()

        val response = client.aggregate(
            AggregateRequest(
                metrics = setOf(StepsRecord.COUNT_TOTAL),
                timeRangeFilter = TimeRangeFilter.between(startOfDay, endOfDay)
            )
        )

        return response[StepsRecord.COUNT_TOTAL] ?: 0L
    }
}
