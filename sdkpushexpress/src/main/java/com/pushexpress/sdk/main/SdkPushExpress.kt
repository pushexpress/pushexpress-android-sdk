package com.pushexpress.sdk.main

import coil.ImageLoader
import android.util.Log
import com.pushexpress.sdk.local_settings.SdkSettingsRepository
import com.pushexpress.sdk.notification.NotificationDrawer
import com.pushexpress.sdk.repository.ApiRepository
import kotlinx.coroutines.*

const val SDK_TAG = "SdkPushExpress"

object SdkPushExpress { 
    private val handler = CoroutineExceptionHandler { _, exception ->
        println("$SDK_TAG: CoroutineExceptionHandler got $exception")
    }
    val scope = CoroutineScope(Dispatchers.IO + SupervisorJob() + handler)
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
        }
    }

    fun setExternalId(externalId: String) {
        runBlocking {
            sdkSettings.savePushExpressExternalId(externalId)
        }
    }

    fun activate() {
        Log.d(SDK_TAG, "Activate called, workflowActivated: $workflowActivated")
        if (workflowActivated) {
            Log.d(SDK_TAG, "Already activated")
            return
        }

        workflowActivated = true
        scope.launch {
            Log.d(SDK_TAG, "Starting API loop")
            try {
                sdkApi.doApiLoop()
            } catch (e: Exception) {
                Log.e(SDK_TAG, "API loop failed: ${e.message}", e)
            }
        }
    }

    fun deactivate() {
        workflowActivated = false
        runBlocking {
            try {
                sdkApi.stopApiLoop()
            } catch (e: Exception) {
                Log.e(SDK_TAG, "Error in stopApiLoop: ${e.message}")
            }
        }
        scope.coroutineContext.cancelChildren()
    }

    fun getInstanceToken() = runBlocking { sdkSettings.getSdkSettings().instanceToken }

    fun getAppId() = runBlocking { sdkSettings.getSdkSettings().appId }

    fun getExternalId() = runBlocking { sdkSettings.getSdkSettings().extId }
}