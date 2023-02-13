package com.pushexpress.sdk.retrofit

import retrofit2.http.Body
import retrofit2.http.POST
import com.pushexpress.sdk.models.DeviceConfigRequest
import com.pushexpress.sdk.models.DeviceConfigResponse
import com.pushexpress.sdk.models.EventsLifecycleRequest
import com.pushexpress.sdk.models.NotificationEventRequest
import retrofit2.Response

internal interface ApiService {

    @POST("devices?origin=droid")
    suspend fun sendDeviceConfig(@Body request: DeviceConfigRequest): DeviceConfigResponse

    @POST("events/lifecycle?origin=droid")
    suspend fun sendLifecycleEvent(@Body request: EventsLifecycleRequest): Response<Unit>

    @POST("events/notification?origin=droid")
    suspend fun sendNotificationEvent(@Body request: NotificationEventRequest): Response<Unit>
}