package com.stepssync.config

object Constants {
    const val WEBHOOK_URL =
        "https://script.google.com/macros/s/AKfycbxz2MCVeBRvU26UwZ9-7nLLq4FyHSPuX6A0HG-54q8nwbickl1WgbyS4uqxjXiuuFenBw/exec"

    const val HEALTH_CONNECT_PACKAGE = "com.google.android.apps.healthdata"
    const val SYNC_WORK_NAME = "steps_sync_daily_worker"
    const val SYNC_REPEAT_INTERVAL_HOURS = 24L
    const val SYNC_FLEX_INTERVAL_HOURS = 1L
    const val SYNC_STATE_PREFS = "steps_sync_state"
    const val LAST_SYNCED_DATE_KEY = "last_synced_date"
    const val LAST_SYNCED_AT_KEY = "last_synced_at"
    const val LAST_SYNCED_STEPS_KEY = "last_synced_steps"
    const val HTTP_TIMEOUT_SECONDS = 20L
}
