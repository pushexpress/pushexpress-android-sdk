package com.pushexpress.sdk.retrofit

import com.pushexpress.sdk.models.DeviceConfigResponse
import com.pushexpress.sdk.models.EventsLifecycleRequest
import com.pushexpress.sdk.models.NotificationEventRequest
import org.json.JSONObject

internal interface ApiService {

    suspend fun getInstanceId()

    suspend fun sendDeviceConfig(config: JSONObject): DeviceConfigResponse

    suspend fun sendLifecycleEvent(event: JSONObject)

    suspend fun sendNotificationEvent(event: JSONObject)
}