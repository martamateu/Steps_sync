package com.stepssync

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

/**
 * Sends step data to the configured Google Apps Script webhook via HTTP POST.
 *
 * Expected JSON payload:
 * ```json
 * { "date": "YYYY-MM-DD", "steps": <number> }
 * ```
 *
 * This object is intentionally simple (no DI framework) to keep the project
 * lightweight. OkHttp manages its own thread pool so calls are safe to make
 * from coroutine-dispatched background threads.
 */
object ApiClient {

    private val httpClient = OkHttpClient()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    /**
     * Posts the daily step count to [Constants.WEBHOOK_URL].
     *
     * @param date  ISO-8601 date string (e.g. "2024-06-15").
     * @param steps Aggregated step count for [date].
     * @throws IOException if the HTTP request fails or the server returns a
     *                     non-2xx response code.
     */
    @Throws(IOException::class)
    fun postSteps(date: String, steps: Long) {
        val payload = JSONObject().apply {
            put("date", date)
            put("steps", steps)
        }.toString()

        val requestBody = payload.toRequestBody(jsonMediaType)

        val request = Request.Builder()
            .url(Constants.WEBHOOK_URL)
            .post(requestBody)
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException(
                    "Webhook returned unexpected status ${response.code} for date=$date steps=$steps"
                )
            }
        }
    }
}
