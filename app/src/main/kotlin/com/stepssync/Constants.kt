package com.stepssync

/**
 * Central configuration constants.
 *
 * Replace [WEBHOOK_URL] with your own Google Apps Script deployment URL.
 * The script must be deployed as "Execute as: Me" and "Who has access: Anyone".
 */
object Constants {

    /**
     * Google Apps Script web-app URL.
     * Deploy your script and paste the /exec URL here.
     */
    const val WEBHOOK_URL =
        "https://script.google.com/macros/s/YOUR_SCRIPT_ID_HERE/exec"

    /** Unique name used to enqueue the periodic WorkManager job. */
    const val SYNC_WORK_NAME = "steps_daily_sync"

    /** How often the sync job should repeat (hours). */
    const val SYNC_INTERVAL_HOURS = 24L
}
