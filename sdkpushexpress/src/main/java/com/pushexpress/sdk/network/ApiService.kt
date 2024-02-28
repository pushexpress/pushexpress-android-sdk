package com.pushexpress.sdk.network

import com.pushexpress.sdk.models.DeviceConfigResponse
import org.json.JSONObject

internal interface ApiService {

    suspend fun getInstanceId()

    suspend fun deactivateDevice()

    suspend fun sendDeviceConfig(config: JSONObject): DeviceConfigResponse

    suspend fun sendLifecycleEvent(event: JSONObject)

    suspend fun sendNotificationEvent(event: JSONObject)
}