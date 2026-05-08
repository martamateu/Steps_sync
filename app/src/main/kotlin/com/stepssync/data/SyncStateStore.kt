package com.stepssync.data

import android.content.Context
import com.stepssync.config.Constants
import java.time.Instant
import java.time.LocalDate

/**
 * Stores the last successfully transmitted completed day.
 *
 * A date is considered "synced" only when the transmitted step count was > 0.
 * If 0 steps were sent (e.g. Health Connect hadn't aggregated data yet), the date
 * is stored with a zero marker so a subsequent run can re-transmit the correct value.
 */
class SyncStateStore(context: Context) {

    private val preferences = context.applicationContext.getSharedPreferences(
        Constants.SYNC_STATE_PREFS,
        Context.MODE_PRIVATE
    )

    /** Returns true only when the date has already been synced with a non-zero step count.
     *
     * A zero-step result is not considered a confirmed sync: Health Connect may not have
     * aggregated the data yet, and a subsequent run should re-attempt with the real value.
     * Once a positive step count has been successfully transmitted, the date is locked to
     * avoid duplicate webhook deliveries.
     */
    fun wasDateSynced(date: LocalDate): Boolean {
        val syncedDate = preferences.getString(Constants.LAST_SYNCED_DATE_KEY, null)
        val syncedSteps = preferences.getLong(Constants.LAST_SYNCED_STEPS_KEY, 0L)
        return syncedDate == date.toString() && syncedSteps > 0L
    }

    fun markDateSynced(date: LocalDate, steps: Long) {
        preferences.edit()
            .putString(Constants.LAST_SYNCED_DATE_KEY, date.toString())
            .putLong(Constants.LAST_SYNCED_STEPS_KEY, steps)
            .putString(Constants.LAST_SYNCED_AT_KEY, Instant.now().toString())
            .apply()
    }
}
