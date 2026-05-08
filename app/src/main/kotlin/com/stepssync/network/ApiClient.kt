package com.stepssync.network

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
        .build()
) {

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun postSteps(date: LocalDate, steps: Long) = withContext(Dispatchers.IO) {
        val payload = JSONObject()
            .put("date", date.toString())
            .put("steps", steps)
            .toString()

        val request = Request.Builder()
            .url(Constants.WEBHOOK_URL)
            .post(payload.toRequestBody(jsonMediaType))
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Webhook error ${response.code} for ${date}")
            }
        }
    }
}
