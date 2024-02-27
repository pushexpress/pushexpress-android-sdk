package com.pushexpress.sdk.network

import android.util.Log
import com.pushexpress.sdk.BuildConfig
import com.pushexpress.sdk.local_settings.SdkSettingsRepository
import com.pushexpress.sdk.main.SDK_TAG
import com.pushexpress.sdk.models.DeviceConfigResponse
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

    object HttpMethod {
        const val POST = "POST"
        const val PUT = "PUT"
    }

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
            val response = withContext(Dispatchers.IO) { client.newCall(request).execute() }
            responseBody = response.body?.string().toString()
            if (BuildConfig.LOG_DEBUG) Log.d(SDK_TAG, "REQUEST: $method\nTO: ${request.url}\nSTATUS: ${response.code}\nDATA: $responseBody")
//            Log.d(SDK_TAG, "$method ${request.url} response: $responseBody")
        } catch (e: Exception) {
            Log.d(SDK_TAG, "$method exception: $e")
        }
        return responseBody
    }

    // r.POST("/apps/:uappId/instances", icInitOrActivateHandler)
    override suspend fun getInstanceId() {
        val instanceToken = settings.getSdkSettings().instanceToken
        val extId = settings.getSdkSettings().extId
        val data = JSONObject().apply{
            put("ic_token", instanceToken)
            put("ext_id", extId)
        }
        val responseBody = makeRequest(HttpMethod.POST, "instances", data)
        try {
            val id = responseBody.let { JSONObject(it).getString("id") }
            Log.d(SDK_TAG, "getInstanceId $responseBody")
            settings.saveDeviceInstanceId(id)
        } catch (e: Exception) {
            Log.d(SDK_TAG, "getInstanceId exception: $e")
        }
    }

    // r.PUT("/apps/:uappId/instances/:icId/info", icUpdateHandler)
    override suspend fun sendDeviceConfig(config: JSONObject): DeviceConfigResponse {
        val instanceId = settings.getSdkSettings().instanceId
        val response = makeRequest(HttpMethod.PUT, "instances/$instanceId/info", config)
        val deviceInterval = response.let { JSONObject(it).getLong("device_intvl") }
        val hbeatInterval = response.let { JSONObject(it).getLong("hbeat_intvl") }
        return DeviceConfigResponse(deviceInterval, hbeatInterval)
    }

    // r.POST("/apps/:uappId/instances/:icId/events/lifecycle", icLifecycleEventHandler)
    override suspend fun sendLifecycleEvent(event: JSONObject) {
        val instanceId = settings.getSdkSettings().instanceId
        makeRequest(HttpMethod.POST, "instances/$instanceId/events/lifecycle", event)
    }

    // r.POST("/apps/:uappId/instances/:icId/events/notification", icNotificationEventHandler)
    override suspend fun sendNotificationEvent(event: JSONObject) {
        val instanceId = settings.getSdkSettings().instanceId
        makeRequest(HttpMethod.POST, "instances/$instanceId/notification", event)
    }

    companion object {
        private const val SDK_PUSHEXPRESS_COMMON_URL = "https://core.push.express/api/r/v2"
    }

}