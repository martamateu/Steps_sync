package com.stepssync.data

import android.content.Context
import com.stepssync.config.Constants
import java.time.Instant
import java.time.LocalDate

/**
 * Stores the last successfully transmitted completed day.
 *
 * This local idempotence strategy assumes the completed-day total is final once sent, so the
 * worker will skip re-posting the same date on later runs to avoid duplicate webhook deliveries.
 */
class SyncStateStore(context: Context) {

    private val preferences = context.applicationContext.getSharedPreferences(
        Constants.SYNC_STATE_PREFS,
        Context.MODE_PRIVATE
    )

    fun wasDateSynced(date: LocalDate): Boolean {
        return preferences.getString(Constants.LAST_SYNCED_DATE_KEY, null) == date.toString()
    }

    fun markDateSynced(date: LocalDate, steps: Long) {
        preferences.edit()
            .putString(Constants.LAST_SYNCED_DATE_KEY, date.toString())
            .putLong(Constants.LAST_SYNCED_STEPS_KEY, steps)
            .putString(Constants.LAST_SYNCED_AT_KEY, Instant.now().toString())
            .apply()
    }
}
