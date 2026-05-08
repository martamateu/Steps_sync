package com.stepssync.data

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
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
        HealthPermission.getReadPermission(StepsRecord::class)
    )

    fun sdkStatus(): Int =
        HealthConnectClient.getSdkStatus(appContext, Constants.HEALTH_CONNECT_PACKAGE)

    fun isAvailable(): Boolean = sdkStatus() == HealthConnectClient.SDK_AVAILABLE

    /** Returns the last fully completed local day, which is the day this app syncs. */
    fun previousDayDate(): LocalDate = LocalDate.now(zoneId).minusDays(1)

    suspend fun hasPermissions(): Boolean {
        if (!isAvailable()) {
            return false
        }
        val grantedPermissions = client.permissionController.getGrantedPermissions()
        return grantedPermissions.containsAll(requiredPermissions)
    }

    suspend fun getPreviousDaySteps(): Long {
        val syncDate = previousDayDate()
        val startTime = syncDate.atStartOfDay(zoneId).toInstant()
        val endTime = syncDate.plusDays(1).atStartOfDay(zoneId).toInstant()

        val response = client.aggregate(
            AggregateRequest(
                metrics = setOf(StepsRecord.COUNT_TOTAL),
                timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
            )
        )

        return response[StepsRecord.COUNT_TOTAL] ?: 0L
    }

    /**
     * Compatibility entry point kept to match the requested project contract.
     *
     * It delegates to [getPreviousDaySteps] so the app always syncs the last fully completed day.
     */
    suspend fun getTodaySteps(): Long = getPreviousDaySteps()
}
