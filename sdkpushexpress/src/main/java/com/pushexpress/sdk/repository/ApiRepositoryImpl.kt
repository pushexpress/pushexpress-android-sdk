package com.pushexpress.sdk.repository

import android.content.Context
import android.os.Build
import android.telephony.TelephonyManager
import android.util.Log
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import androidx.core.app.NotificationManagerCompat
import com.pushexpress.sdk.local_settings.SdkSettingsRepository
import com.pushexpress.sdk.retrofit.RetrofitBuilder
import com.pushexpress.sdk.utils.retryHttpIO
import retrofit2.HttpException
import kotlinx.coroutines.*
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

import com.pushexpress.sdk.BuildConfig
import com.pushexpress.sdk.main.SDK_TAG
import com.pushexpress.sdk.main.SdkPushExpress
import com.pushexpress.sdk.main.SdkPushExpress.workflowActivated
import com.pushexpress.sdk.models.*

internal class ApiRepositoryImpl(
    private val context: Context,
    private val settingsRepository: SdkSettingsRepository
) : ApiRepository {

    private val builder = RetrofitBuilder(SDK_PUSHEXPRESS_COMMON_URL)
    private val sdkService = builder.sdkService
    private var devicesJob: Job = Job()
    private var heartBeatsJob: Job = Job()
  

    private val eventQueue = ConcurrentLinkedQueue<Pair<String, NotificationEvent>>()
    private val eventMutex = Mutex()

    private val handler = CoroutineExceptionHandler { _, exception ->
        println("$SDK_TAG: CoroutineExceptionHandler got $exception")
    }

    private val scope = SdkPushExpress.scope

    override suspend fun doApiLoop() {
        if (BuildConfig.LOG_RELEASE) Log.d(SDK_TAG, "ApiLoop iteration started")
        try {
            val res = retryHttpIO(times = 10) { createAndSendDeviceConfig() }
            repeatRequestDevices(120)
            repeatRequestHeartBeat(30)
        } catch (e: Exception) {
            if (BuildConfig.LOG_RELEASE) Log.d(SDK_TAG, "ApiLoop: unhandled error: $e")
        }
    }

    override suspend fun stopApiLoop() {
        if (BuildConfig.LOG_RELEASE) Log.d(SDK_TAG, "ApiLoop stopped")

        Log.d(SDK_TAG, "Getting settings...")
        val sdkSettings = settingsRepository.getSdkSettings()
    
        val instanceId = settingsRepository.getInstanceId()
        Log.d(SDK_TAG, "After getInstanceId(): $instanceId")

        if (instanceId != null) {
            val request = RegisterInstanceRequest(
                ic_token = settingsRepository.getUuidv4() ?: "",
                ext_id = sdkSettings.extId
            )
            try {
                val response = sdkService.deactivateInstance(
                    appId = sdkSettings.appId,
                    instanceId = instanceId,
                    request = request
                )
                if (BuildConfig.LOG_DEBUG) Log.d(SDK_TAG, "kfkgdlgkl : ${request}")
                if (BuildConfig.LOG_DEBUG) Log.d(SDK_TAG, "✅ Instance deactivated: ${response.code()}")
            } catch (e: Exception) {
                Log.e(SDK_TAG, "❌ Failed to deactivate instance", e)
            }
        } else {
            if (BuildConfig.LOG_DEBUG) Log.d(SDK_TAG, "⚠️ No instanceId to deactivate")
        }

        try {
            settingsRepository.clearInstanceIdCache()
            if (BuildConfig.LOG_DEBUG) Log.d(SDK_TAG, "✅ InstanceId cache cleared")
            settingsRepository.deleteExternalId()
            if (BuildConfig.LOG_DEBUG) Log.d(SDK_TAG, "✅ deleteExternalId cleared")
        } catch (e: Exception) {
            Log.e(SDK_TAG, "❌ Failed to clear instanceId cache", e)
        }

        try {
            eventMutex.withLock {
                eventQueue.clear()
            }
            if (BuildConfig.LOG_DEBUG) Log.d(SDK_TAG, "✅ Event queue cleared: ${eventQueue.size} events")
        } catch (e: Exception) {
                Log.e(SDK_TAG, "❌ Failed to clear event queue", e)
        }

        try {
            settingsRepository.deleteInstanceId()
            if (BuildConfig.LOG_DEBUG) Log.d(SDK_TAG, "✅ InstanceId removed from DataStore")
        } catch (e: Exception) {
            Log.e(SDK_TAG, "❌ Failed to remove instanceId from DataStore", e)
        }

        try {
            devicesJob.cancel("Stopping API loop")
            heartBeatsJob.cancel("Stopping API loop")
            if (BuildConfig.LOG_DEBUG) Log.d(SDK_TAG, "✅ Jobs cancelled")
        } catch (e: Exception) {
            Log.e(SDK_TAG, "❌ Error cancelling jobs", e)
        }

        if (BuildConfig.LOG_DEBUG) Log.d(SDK_TAG, "✅ Workflow deactivated")
        if (BuildConfig.LOG_RELEASE) Log.d(SDK_TAG, "🛑 ApiLoop completely stopped and cleaned")
    }

    override fun sendLifecycleEvent(event: EventsLifecycle) {
        scope.launch {
            if (BuildConfig.LOG_DEBUG) Log.d(SDK_TAG, "sendLifecycleEvent[${event.event}]")

            if (event == EventsLifecycle.ONSCREEN) settingsRepository.updateAppResumed()
            else if (event == EventsLifecycle.BACKGROUND) settingsRepository.updateAppStopped()

            if (!workflowActivated) return@launch

            val settings = settingsRepository.getSdkSettings()
            val instanceId = settingsRepository.getInstanceId()
            if (instanceId != null) {
                if (BuildConfig.LOG_RELEASE) Log.d(SDK_TAG, "Send LifecycleEvent")
                sdkService.sendLifecycleEvent(settings.appId, instanceId)
            }
        }
    }

    override fun saveFirebaseToken(token: String) {
        scope.launch {
            settingsRepository.saveFirebaseToken(token)
            if (BuildConfig.LOG_DEBUG) Log.d(SDK_TAG, "saveFirebaseToken: saved to settings")
        }
    }

    override fun sendNotificationEvent(messageId: String, event: NotificationEvent) {
        scope.launch {
            if (BuildConfig.LOG_DEBUG) Log.d(SDK_TAG,
                "sendNotificationEvent[$messageId, ${event.event}]")

            if (!workflowActivated) {
                workflowActivated = true
                Log.d(SDK_TAG, "✅ Workflow activated by first notification event")
            }

            val instanceId = settingsRepository.getInstanceId()

            if (instanceId == null) {
                addToQueue(messageId, event)
                if (BuildConfig.LOG_DEBUG) Log.d(SDK_TAG, "📦 Event queued: $messageId, queue size: ${eventQueue.size}")
                return@launch
            }

            sendEventImmediately(messageId, event, instanceId)
        }
    }

    private suspend fun addToQueue(messageId: String, event: NotificationEvent) {
        eventMutex.withLock {
            eventQueue.add(Pair(messageId, event))
        }
    }

    private suspend fun sendEventImmediately(
        messageId: String, 
        event: NotificationEvent, 
        instanceId: String
    ) {
        try {
            val sdkSettings = settingsRepository.getSdkSettings()
            val evt = NotificationEventRequest(
                msg_id = messageId,
                event = event.event
            )
            
            if (BuildConfig.LOG_RELEASE) Log.d(SDK_TAG, "Send NotificationEvent Check: ${sdkSettings.appId}, $instanceId, $evt")
            
            val response = sdkService.sendNotificationEvent(sdkSettings.appId, instanceId, evt)
            
            if (BuildConfig.LOG_DEBUG) Log.d(SDK_TAG, "Server response: ${response.code()} - ${response.body()}")
            Log.d(SDK_TAG, "✅ Success! Response: ${response.code()}")
            
        } catch (e: Exception) {
            Log.e(SDK_TAG, "❌ Error sending event: $messageId", e)
            if (e is HttpException && (e.code() == 404 || e.code() == 400)) {
                if (BuildConfig.LOG_DEBUG) Log.d(SDK_TAG, "Invalid instance, clearing cache")
            //    settingsRepository.clearInstanceIdCache()
            }
        }
    }

    private suspend fun processEventQueue() {
        val instanceId = settingsRepository.getInstanceId() ?: return
        
        eventMutex.withLock {
            val iterator = eventQueue.iterator()
            while (iterator.hasNext()) {
                val eventPair = iterator.next()
                val (messageId, event) = eventPair
                sendEventImmediately(messageId, event, instanceId)
                iterator.remove()
                delay(50)
            }
            
            if (BuildConfig.LOG_DEBUG) Log.d(SDK_TAG, "✅ Event queue processed")
        }
    }

    fun getEventQueueSize(): Int = eventQueue.size

    private fun repeatRequestDevices(timeSec: Long) {
        devicesJob = scope.launch {
            try {
                 while (isActive) {
                    if (BuildConfig.LOG_DEBUG) Log.d(SDK_TAG, "repeatRequestDevices")
                    delay(timeSec * 1000)
                    if (isActive) {
                        doApiLoop()
                    }
                 }
            }  catch (e: CancellationException) {
                if (BuildConfig.LOG_DEBUG) Log.d(SDK_TAG, "Devices job cancelled normally")
            } catch (e: Exception) {
                Log.e(SDK_TAG, "Devices job error", e)
            }
        }
    }

    private fun repeatRequestHeartBeat(timeSec: Long) {
    heartBeatsJob = scope.launch {
        try {
            while (isActive) {
                delay(timeSec * 1000)
                if (isActive) {
                    sendLifecycleEvent(EventsLifecycle.HBEAT)
                }
            }
        } catch (e: CancellationException) {
            if (BuildConfig.LOG_DEBUG) Log.d(SDK_TAG, "Heartbeat job cancelled normally")
        } catch (e: Exception) {
            Log.e(SDK_TAG, "Heartbeat job error", e)
        }
    }
}

    private suspend fun createAndSendDeviceConfig(): DeviceConfigResponse {
        if (BuildConfig.LOG_DEBUG) Log.d(SDK_TAG, "sendDeviceConfig")
        
        val sdkSettings = settingsRepository.getSdkSettings()
        
        if (sdkSettings.instanceToken.isNotEmpty()) {
            val savedInstanceId = settingsRepository.getInstanceId()
            
            if (savedInstanceId != null && savedInstanceId.isNotEmpty()) {
                try {
                    if (BuildConfig.LOG_DEBUG) Log.d(SDK_TAG, "Using cached instanceId: $savedInstanceId")
                    return sendDeviceInfo(savedInstanceId)
                } catch (e: Exception) {
                    if (e is HttpException && (e.code() == 404 || e.code() == 400)) {
                        if (BuildConfig.LOG_DEBUG) Log.d(SDK_TAG, "Cached instance invalid, re-registering. Error: ${e.message}")
                    } else {
                        throw e
                    }
                }
            }
        }
        
        if (BuildConfig.LOG_DEBUG) Log.d(SDK_TAG, "Performing full instance registration")
        val instanceId = registerInstance()

        if (instanceId.isEmpty()) {
            throw Exception("Failed to register instance: received empty instanceId")
        }
        
        settingsRepository.saveInstanceId(instanceId)
        
        if (BuildConfig.LOG_DEBUG) Log.d(SDK_TAG, "Successfully registered new instance: $instanceId")
        return sendDeviceInfo(instanceId)
    }

    private suspend fun registerInstance(): String {
        val sdkSettings = settingsRepository.getSdkSettings()

        val uuidv4 = settingsRepository.getUuidv4() ?: UUID.randomUUID().toString().also {
            settingsRepository.saveUuidv4(it)
            if (BuildConfig.LOG_DEBUG) Log.d(SDK_TAG, "Generated new UUIDv4: $it")
        }
            
        if (BuildConfig.LOG_DEBUG && settingsRepository.getUuidv4() != null) {
            Log.d(SDK_TAG, "Using existing UUIDv4: $uuidv4")
        }
        
        val request = RegisterInstanceRequest(
            ic_token = uuidv4,
            ext_id = sdkSettings.extId
        )
        
        if (BuildConfig.LOG_RELEASE) Log.d(SDK_TAG, "Registering instance: $request")
        
        val response = sdkService.registerInstance(
            appId = sdkSettings.appId, 
            request = request
        )
        
        if (response.isSuccessful) {
            val instanceResponse = response.body()
            if (instanceResponse != null) {
                settingsRepository.saveInstanceId(instanceResponse.id)
                settingsRepository.saveUuidv4(uuidv4)
                
                if (BuildConfig.LOG_DEBUG) Log.d(SDK_TAG, 
                    "Instance registered: id=${instanceResponse.id}, just_created=${instanceResponse.just_created}")
                
                return instanceResponse.id
            } else {
                throw Exception("Empty response body from instance registration")
            }
        } else {
            throw Exception("Instance registration failed: ${response.code()} - ${response.message()}")
        }
    }

    private suspend fun sendDeviceInfo(instanceId: String): DeviceConfigResponse {
    val advId = try {
        AdvertisingIdClient.getAdvertisingIdInfo(context).id ?: ""
    } catch (e: Exception) {
        ""
    }
    
    val sdkSettings = settingsRepository.getSdkSettings()
    val firebaseToken = sdkSettings.firebaseToken
    
    if (firebaseToken.isEmpty()) {
        if (BuildConfig.LOG_RELEASE) Log.d(SDK_TAG, 
            "⚠️ Firebase token is empty. Call SdkPushExpress.setFirebaseToken() to set it.")
    }

    val areNotificationsEnabled = areNotificationsEnabled()
    
    val request = DeviceInfoRequest(
        transport_type = "fcm.data",
        transport_token = firebaseToken,
        platform_type = "android",
        lang = Locale.getDefault().language,
        agent_name = BuildConfig.VERSION_NAME,
        tz_sec = TimeZone.getDefault().rawOffset / 1000,
        notif_perm_granted = areNotificationsEnabled,
        tags = DeviceTags(
            adID = advId,
            segment = "",
            webmaster = ""
        )
    )
    
    if (BuildConfig.LOG_RELEASE) {
        Log.d(SDK_TAG, "Sending device info: $request, instanceId: $instanceId, appId: ${sdkSettings.appId}")
    }

    try {
        val deviceConfigResponse = sdkService.sendDeviceInfo(sdkSettings.appId, instanceId, request)
        
        if (BuildConfig.LOG_DEBUG) Log.d(SDK_TAG, 
            "Device info sent successfully: $deviceConfigResponse")
        
        return deviceConfigResponse
    } catch (e: Exception) {
        if (e is HttpException) {
            throw Exception("Device info sending failed: ${e.code()} - ${e.message()}")
        } else {
            throw Exception("Device info sending failed: ${e.message}")
        }
    }
}

    private fun areNotificationsEnabled(): Boolean {
        return try {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.areNotificationsEnabled()
        } catch (e: Exception) {
            if (BuildConfig.LOG_DEBUG) {
                Log.e(SDK_TAG, "Failed to check notification permission", e)
            }
            false
        }
    }


    private fun getCountrySim(): String {
        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        return tm.simCountryIso
    }

    private fun getCountryCode(): String {
        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        return tm.networkCountryIso.uppercase()
    }

    companion object {
        private const val SDK_PUSHEXPRESS_COMMON_URL = "https://core.push.express/api/r/v2/"
    }
}