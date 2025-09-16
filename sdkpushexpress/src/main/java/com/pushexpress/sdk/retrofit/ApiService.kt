package com.pushexpress.sdk.retrofit

import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import com.pushexpress.sdk.models.RegisterInstanceRequest
import com.pushexpress.sdk.models.RegisterInstanceResponse
import com.pushexpress.sdk.models.DeviceInfoRequest
import com.pushexpress.sdk.models.DeviceConfigResponse
import com.pushexpress.sdk.models.NotificationEventRequest
import retrofit2.Response

internal interface ApiService {

    @POST("apps/{appId}/instances/{instanceId}/events/lifecycle?origin=droid")
    suspend fun sendLifecycleEvent(
        @Path("appId") appId: String,
        @Path("instanceId") instanceId: String,
    ): Response<Unit>

    @POST("apps/{appId}/instances/{instanceId}/events/notification?origin=droid")
    suspend fun sendNotificationEvent(
        @Path("appId") appId: String,
        @Path("instanceId") instanceId: String,
        @Body request: NotificationEventRequest
    ): Response<Unit>

    @POST("apps/{appId}/instances?origin=droid")
    suspend fun registerInstance(
        @Path("appId") appId: String,
        @Body request: RegisterInstanceRequest
    ): Response<RegisterInstanceResponse>

    @POST("apps/{appId}/instances/{instanceId}/deactivate?origin=droid")
    suspend fun deactivateInstance(
        @Path("appId") appId: String,
        @Path("instanceId") instanceId: String,
        @Body request: RegisterInstanceRequest
    ): Response<RegisterInstanceResponse>

    @PUT("apps/{appId}/instances/{instanceId}/info?origin=droid")
    suspend fun sendDeviceInfo(
        @Path("appId") appId: String,
        @Path("instanceId") instanceId: String,
        @Body request: DeviceInfoRequest
    ): DeviceConfigResponse
}