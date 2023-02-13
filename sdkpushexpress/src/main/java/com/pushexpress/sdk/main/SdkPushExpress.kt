package com.pushexpress.sdk.main

import coil.ImageLoader
import com.pushexpress.sdk.local_settings.SdkSettingsRepository
import com.pushexpress.sdk.repository.ApiRepository
import kotlinx.coroutines.*

object SdkPushExpress {
    private val handler = CoroutineExceptionHandler { _, exception ->
        println("SdkPushExpress: CoroutineExceptionHandler got $exception")
    }
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob() + handler)

    internal lateinit var sdkApi: ApiRepository
    internal lateinit var sdkSettings: SdkSettingsRepository
    internal lateinit var imageLoader: ImageLoader

    internal fun start() {
        scope.launch {
            sdkApi.doApiLoop()
        }
    }

    fun setAppId(appId: String) {
        runBlocking {
            sdkSettings.savePushExpressAppId(appId)
        }
        scope.launch {
            sdkApi.sendDeviceConfig()
        }
    }

    fun setExternalId(externalId: String) {
        runBlocking {
            sdkSettings.savePushExpressExternalId(externalId)
        }
        scope.launch {
            sdkApi.sendDeviceConfig()
        }
    }

    fun getInstanceToken() = runBlocking { sdkSettings.getSdkSettings().instanceToken }

    fun getExternalId() = runBlocking { sdkSettings.getSdkSettings().extId }
}