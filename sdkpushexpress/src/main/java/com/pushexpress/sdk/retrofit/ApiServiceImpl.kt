package com.pushexpress.sdk.retrofit

import android.util.Log
import com.pushexpress.sdk.local_settings.SdkSettingsRepository
import com.pushexpress.sdk.main.SDK_TAG
import com.pushexpress.sdk.models.DeviceConfigResponse
import com.pushexpress.sdk.models.EventsLifecycleRequest
import com.pushexpress.sdk.models.NotificationEventRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class ApiServiceImpl(
    private val client: OkHttpClient,
    private val settings: SdkSettingsRepository
): ApiService {
    private val baseUrl = SDK_PUSHEXPRESS_COMMON_URL

    private suspend fun makeRequest(
        method: String,
        urlSuffix: String,
        data: JSONObject
    ): String {
        val appId = settings.getSdkSettings().appId
        var responseBody = ""
        val commonUrl = "$baseUrl/apps/$appId"
        try {
            val requestBody =
                data.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
            val request = Request.Builder()
                .url("$commonUrl/$urlSuffix")
                .method(method, requestBody)
                .build()

            Log.d(SDK_TAG, "$method ${request.url} request: $data")

            val response = withContext(Dispatchers.IO) { client.newCall(request).execute() }
            responseBody = response.body?.string().toString()
            Log.d(SDK_TAG, "$method ${request.url} response: $responseBody")
            Log.d(SDK_TAG, "${::makeRequest.name}${response.code}")
        } catch (e: Exception) {
            Log.d(SDK_TAG, "$method exception: $e")
        }
        return responseBody
    }

    override suspend fun getInstanceId() {
        val instanceToken = settings.getSdkSettings().instanceToken
        val extId = settings.getSdkSettings().extId
        val data = JSONObject().apply{
            put("ic_token", instanceToken)
            put("ext_id", extId)
        }
        val responseBody = makeRequest("POST", "instances", data)
        try {
            val id = responseBody.let { JSONObject(it).getString("id") }
            Log.d(SDK_TAG, "getInstanceId $responseBody")
            settings.saveDeviceInstanceId(id)
        } catch (e: Exception) {
            Log.d(SDK_TAG, "getInstanceId exception: $e")
        }
    }

    override suspend fun sendDeviceConfig(config: JSONObject): DeviceConfigResponse {
        val instanceId = settings.getSdkSettings().instanceId
        Log.d(SDK_TAG, "INSTANCE ID $instanceId")
        makeRequest("PUT", "instances/$instanceId/info", config)
        Log.d(SDK_TAG, config.toString())
        return DeviceConfigResponse(123123, 41251254)
    }

    override suspend fun sendLifecycleEvent(event: JSONObject) {
        val instanceId = settings.getSdkSettings().instanceId
        Log.d(SDK_TAG, "Sending event")
        makeRequest("POST", "instances/$instanceId/events/lifecycle", event)
    }

    override suspend fun sendNotificationEvent(event: JSONObject) {
    TODO()
    }

    companion object {
        private const val SDK_PUSHEXPRESS_COMMON_URL = "https://core.push.express/api/r/v2"
    }

}