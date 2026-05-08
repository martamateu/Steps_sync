package com.stepssync.network

import android.util.Log
import com.stepssync.config.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.time.LocalDate
import java.util.concurrent.TimeUnit

class ApiClient(
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(Constants.HTTP_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(Constants.HTTP_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(Constants.HTTP_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        // Google Apps Script /exec always returns a 302 redirect.
        // OkHttp's default redirect handling converts POST→GET, which drops the JSON body.
        // Disabling redirects means we treat the 302 as a success (the script has already run).
        .followRedirects(false)
        .followSslRedirects(false)
        .build()
) {

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun postSteps(date: LocalDate, steps: Long, calories: Double) = withContext(Dispatchers.IO) {
        val payload = JSONObject()
            .put("date", date.toString())
            .put("steps", steps)
            .put("calories", Math.round(calories))
            .toString()

        Log.d(TAG, "Posting to webhook: $payload")

        val request = Request.Builder()
            .url(Constants.WEBHOOK_URL)
            .post(payload.toRequestBody(jsonMediaType))
            .build()

        httpClient.newCall(request).execute().use { response ->
            Log.i(TAG, "Webhook response code: ${response.code} for $date")
            // 2xx = direct success; 3xx = GAS redirect (script already ran successfully)
            if (!response.isSuccessful && response.code !in 300..399) {
                val body = runCatching { response.body?.string() }.getOrElse { "body read failed: ${it.message}" }
                throw IOException("Webhook error ${response.code} for $date – body: $body")
            }
        }
    }

    companion object {
        private const val TAG = "ApiClient"
    }
}
