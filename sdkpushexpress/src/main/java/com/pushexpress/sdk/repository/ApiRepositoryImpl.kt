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

import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.ktx.messaging
import com.pushexpress.sdk.BuildConfig
import com.pushexpress.sdk.main.SDK_TAG
import com.pushexpress.sdk.main.SdkPushExpress.workflowActivated
import com.pushexpress.sdk.models.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

internal class ApiRepositoryImpl(
    private val context: Context,
    private val settingsRepository: SdkSettingsRepository
) : ApiRepository {

    private val builder = RetrofitBuilder(SDK_PUSHEXPRESS_COMMON_URL)
    private val sdkService = builder.sdkService
    private var devicesJob: Job = Job()
    private var heartBeatsJob: Job = Job()
    private var commonJob: Job = SupervisorJob()

    private val handler = CoroutineExceptionHandler { _, exception ->
        println("$SDK_TAG: CoroutineExceptionHandler got $exception")
    }
    private val scope = CoroutineScope(Dispatchers.IO + commonJob + handler)

    override suspend fun doApiLoop() =
        withContext(scope.coroutineContext) {
            if (BuildConfig.LOG_RELEASE) Log.d(SDK_TAG, "ApiLoop iteration started")

            devicesJob.cancel()
            heartBeatsJob.cancel()
            try {
                val res = retryHttpIO(times = 10) { createAndSendDeviceConfig() }
                repeatRequestDevices(res.device_intvl)
                repeatRequestHeartBeat(res.hbeat_intvl)
            } catch (e: Exception) {
                if (BuildConfig.LOG_RELEASE) Log.d(SDK_TAG, "ApiLoop: unhandled error: $e")
                // repeat loop now
                repeatRequestDevices(1)
            }
        }

    override suspend fun stopApiLoop() =
        withContext(scope.coroutineContext) {
            if (BuildConfig.LOG_RELEASE) Log.d(SDK_TAG, "ApiLoop stopped")

            devicesJob.cancel()
            heartBeatsJob.cancel()
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

            if (!workflowActivated) return@launch

            val sdkSettings = settingsRepository.getSdkSettings()
            val instanceId = settingsRepository.getInstanceId()
            if (instanceId != null) {
                val evt = NotificationEventRequest(
                    msg_id = messageId,
                    event = event.event
                )
                if (BuildConfig.LOG_RELEASE) Log.d(SDK_TAG, "Send NotificationEvent: ${evt}")
                sdkService.sendNotificationEvent(sdkSettings.appId, instanceId, evt)
            }
        }
    }

    private fun repeatRequestDevices(timeSec: Long) {
        devicesJob = scope.launch {
            if (BuildConfig.LOG_DEBUG) Log.d(SDK_TAG, "repeatRequestDevices")
            timeSec.let {
                delay(it * 1000)
                ensureActive()
                doApiLoop()
            }
        }
    }

    private fun repeatRequestHeartBeat(timeSec: Long) {
        heartBeatsJob = scope.launch {
            if (BuildConfig.LOG_DEBUG) Log.d(SDK_TAG, "repeatRequestHeartBeat")
            timeSec.let {
                while (isActive) {
                    delay(it * 1000)
                    ensureActive()
                    sendLifecycleEvent(EventsLifecycle.HBEAT)
                }
            }
        }
    }

    private suspend fun createAndSendDeviceConfig(): DeviceConfigResponse {
        if (BuildConfig.LOG_DEBUG) Log.d(SDK_TAG, "sendDeviceConfig")
        
        val sdkSettings = settingsRepository.getSdkSettings()
        
        if (sdkSettings.instanceToken.isNotEmpty()) {
            val savedInstanceId = settingsRepository.getInstanceId()
            
            if (savedInstanceId != null) {
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
        return sendDeviceInfo(instanceId)
    }

    private suspend fun registerInstance(): String {
        val sdkSettings = settingsRepository.getSdkSettings()
        
        val request = RegisterInstanceRequest(
            ic_token = sdkSettings.instanceToken,
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
        val firebaseToken = if (sdkSettings.firebaseToken.isEmpty()) {
            getFirebaseToken()
        } else {
            sdkSettings.firebaseToken
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
        
        if (BuildConfig.LOG_RELEASE) Log.d(SDK_TAG, "Sending device info: $request,$instanceId,$sdkSettings.appId" )
        
        return sdkService.sendDeviceInfo(sdkSettings.appId, instanceId, request)
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

    private suspend fun getFirebaseToken(): String {
        return suspendCoroutine { continuation ->
            Firebase.messaging.token.addOnCompleteListener {
                if (it.isSuccessful) {
                    if (BuildConfig.LOG_DEBUG) Log.d(SDK_TAG,
                        "getFirebaseToken: token=${it.result}")
                    saveFirebaseToken(it.result)
                    continuation.resume(it.result)
                } else {
                    continuation.resume("")
                }
            }
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