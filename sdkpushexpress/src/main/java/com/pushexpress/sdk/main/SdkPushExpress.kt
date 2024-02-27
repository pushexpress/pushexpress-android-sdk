package com.pushexpress.sdk.main

import coil.ImageLoader
import com.pushexpress.sdk.local_settings.SdkSettingsRepository
import com.pushexpress.sdk.notification.NotificationDrawer
import com.pushexpress.sdk.repository.ApiRepository
import kotlinx.coroutines.*

const val SDK_TAG = "SdkPushExpress"

object SdkPushExpress {
    private val handler = CoroutineExceptionHandler { _, exception ->
        println("$SDK_TAG: CoroutineExceptionHandler got $exception")
    }
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob() + handler)
    internal lateinit var sdkApi: ApiRepository
    internal lateinit var sdkSettings: SdkSettingsRepository
    internal lateinit var imageLoader: ImageLoader
    internal lateinit var notificationDrawer: NotificationDrawer

    @Volatile
    internal var workflowActivated: Boolean = false

    @Deprecated("Deprecated. Use initialize(id); activate() instead.",
        ReplaceWith("initialize(id); activate()"))
    fun setAppId(appId: String) {
        scope.launch {
            sdkSettings.savePushExpressAppId(appId)
        }
        activate()
    }

    fun initialize(appId: String) {
        scope.launch {
            sdkSettings.savePushExpressAppId(appId)
            sdkApi.getInstanceId()
        }
    }

    fun setTag(tagKey: String, tagValue: String) {
        scope.launch {
            sdkSettings.setTag(tagKey, tagValue)
        }
    }

    fun setExternalId(externalId: String) {
        scope.launch {
            sdkSettings.savePushExpressExternalId(externalId)
        }
    }

    fun activate() {
        if (workflowActivated) return
        workflowActivated = true
        scope.launch {
            sdkApi.doApiLoop()
        }
    }

    fun deactivate() {
        if (!workflowActivated) return
        workflowActivated = false
        scope.launch {
            sdkApi.stopApiLoop()
        }
    }

    fun getInstanceToken() = runBlocking { sdkSettings.getSdkSettings().instanceToken }

    fun getAppId() = runBlocking { sdkSettings.getSdkSettings().appId }

    fun getExternalId() = runBlocking { sdkSettings.getSdkSettings().extId }
}