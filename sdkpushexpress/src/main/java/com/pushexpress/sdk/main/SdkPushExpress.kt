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

    suspend fun initialize(appId: String) {
        sdkSettings.savePushExpressAppId(appId)
    }

    suspend fun setExternalId(externalId: String) {
        sdkSettings.savePushExpressExternalId(externalId)
    }

    suspend fun setFirebaseToken(token: String) {
        sdkApi.saveFirebaseToken(token)
        Log.d(SDK_TAG, "Firebase token set successfully")
    }

    suspend fun activate() {
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

    suspend fun deactivate() {
        workflowActivated = false
        try {
            sdkApi.stopApiLoop()
        } catch (e: Exception) {
            Log.e(SDK_TAG, "Error in stopApiLoop: ${e.message}")
        }
    }

    suspend fun getInstanceToken(): String = sdkSettings.getSdkSettings().instanceToken

    suspend fun getAppId(): String = sdkSettings.getSdkSettings().appId 

    suspend fun getExternalId(): String = sdkSettings.getSdkSettings().extId
}