package com.pushexpress.sdk.repository

import com.pushexpress.sdk.models.DeviceConfigResponse
import com.pushexpress.sdk.models.EventsLifecycle
import com.pushexpress.sdk.models.NotificationEvent

interface ApiRepository {
    suspend fun doApiLoop()

    suspend fun sendDeviceConfig(): DeviceConfigResponse

    suspend fun sendLifecycleEvent(event: EventsLifecycle)

    fun saveFirebaseToken(token: String)

    fun sendNotificationEvent(messageId: String, event: NotificationEvent)
}