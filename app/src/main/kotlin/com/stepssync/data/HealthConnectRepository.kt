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

    fun targetDate(): LocalDate = LocalDate.now(zoneId).minusDays(1)

    suspend fun hasPermissions(): Boolean {
        if (!isAvailable()) {
            return false
        }
        val grantedPermissions = client.permissionController.getGrantedPermissions()
        return grantedPermissions.containsAll(requiredPermissions)
    }

    suspend fun getTodaySteps(): Long {
        val targetDate = targetDate()
        val startTime = targetDate.atStartOfDay(zoneId).toInstant()
        val endTime = targetDate.plusDays(1).atStartOfDay(zoneId).toInstant()

        val response = client.aggregate(
            AggregateRequest(
                metrics = setOf(StepsRecord.COUNT_TOTAL),
                timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
            )
        )

        return response[StepsRecord.COUNT_TOTAL] ?: 0L
    }
}
