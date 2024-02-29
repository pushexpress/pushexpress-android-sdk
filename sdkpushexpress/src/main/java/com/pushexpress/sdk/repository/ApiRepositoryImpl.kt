package com.pushexpress.sdk.repository

import android.content.Context
import android.os.Build
import android.telephony.TelephonyManager
import android.util.Log
import com.google.android.gms.ads.identifier.AdvertisingIdClient.getAdvertisingIdInfo
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.ktx.messaging
import com.pushexpress.sdk.BuildConfig
import com.pushexpress.sdk.local_settings.SdkSettingsRepository
import com.pushexpress.sdk.main.SDK_TAG
import com.pushexpress.sdk.main.SdkPushExpress.workflowActivated
import com.pushexpress.sdk.models.DeviceConfigResponse
import com.pushexpress.sdk.models.EventsLifecycle
import com.pushexpress.sdk.models.NotificationEvent
import com.pushexpress.sdk.network.ApiServiceImpl
import com.pushexpress.sdk.network.HttpClient
import com.pushexpress.sdk.utils.retryHttpIO
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.Locale
import java.util.TimeZone
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

internal class ApiRepositoryImpl(
    private val context: Context,
    private val settingsRepository: SdkSettingsRepository
) : ApiRepository {

    private val client = HttpClient.client
    private val sdkService = ApiServiceImpl(client, settingsRepository)
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
                repeatRequestDevices(res.update_interval_sec)
            } catch (e: Exception) {
                if (BuildConfig.LOG_RELEASE) Log.d(SDK_TAG, "ApiLoop: unhandled error: $e")
                // repeat loop now
                repeatRequestDevices(1)
            }
        }

    override suspend fun deactivateDevice() {
        sdkService.deactivateDevice()
    }

    override suspend fun stopApiLoop() =
        withContext(scope.coroutineContext) {
            if (BuildConfig.LOG_RELEASE) Log.d(SDK_TAG, "ApiLoop stopped")

            devicesJob.cancel()
            heartBeatsJob.cancel()
        }

    // send only once after appId or ExtId changes
    /*override suspend fun sendDeviceConfig() =
        withContext(scope.coroutineContext) {
            createAndSendDeviceConfig()
        }
    */

    override fun sendLifecycleEvent(event: EventsLifecycle) {
        scope.launch {
            if (BuildConfig.LOG_DEBUG) Log.d(SDK_TAG, "sendLifecycleEvent[${event.event}]")

            if (event == EventsLifecycle.ONSCREEN) settingsRepository.updateAppResumed()
            else if (event == EventsLifecycle.BACKGROUND) settingsRepository.updateAppStopped()

            if (!workflowActivated) return@launch

            val evt = JSONObject().apply {
                put("event", event.event)
            }
            if (BuildConfig.LOG_RELEASE) Log.d(SDK_TAG, "Send LifecycleEvent: $evt")
            sdkService.sendLifecycleEvent(evt)
        }
    }

    override fun saveFirebaseToken(token: String) {
        scope.launch {
            settingsRepository.saveFirebaseToken(token)
            if (BuildConfig.LOG_DEBUG) Log.d(SDK_TAG, "saveFirebaseToken: saved to settings")
        }
    }

    override suspend fun getInstanceId() {
        sdkService.getInstanceId()
    }

    override fun sendNotificationEvent(messageId: String, event: NotificationEvent) {
        scope.launch {
            if (BuildConfig.LOG_DEBUG) Log.d(SDK_TAG,
                "sendNotificationEvent[$messageId, ${event.event}]")

            if (!workflowActivated) return@launch

            val evt = JSONObject().apply{
                put("event", event.event)
                put("msg_id", messageId)
            }

            if (BuildConfig.LOG_RELEASE) Log.d(SDK_TAG, "Send NotificationEvent: $evt")
            sdkService.sendNotificationEvent(evt)
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

//    private fun repeatRequestHeartBeat(timeSec: Long) {
//        heartBeatsJob = scope.launch {
//            if (BuildConfig.LOG_DEBUG) Log.d(SDK_TAG, "repeatRequestHeartBeat")
//            timeSec.let {
//                while (isActive) {
//                    delay(it * 1000)
//                    ensureActive()
//                    sendLifecycleEvent(EventsLifecycle.HBEAT)
//                }
//            }
//        }
//    }

    private suspend fun createAndSendDeviceConfig(): DeviceConfigResponse {
        if (BuildConfig.LOG_DEBUG) Log.d(SDK_TAG, "sendDeviceConfig")
        val dc = createDevicesRequest()
        if (BuildConfig.LOG_RELEASE) Log.d(SDK_TAG, "Send DeviceConfig: $dc")
        return sdkService.sendDeviceConfig(dc)
    }

    private suspend fun createDevicesRequest(): JSONObject {
        val advId = try {
            getAdvertisingIdInfo(context).id.orEmpty()
        } catch (e: Exception) {
            null
        }
        val sdkSettings = settingsRepository.getSdkSettings()

        val deviceConfig = JSONObject().apply {
            put("app_id", sdkSettings.appId)
            put("ic_token", sdkSettings.instanceToken)
            put("fcm_token", sdkSettings.firebaseToken.let {
                if (it.isEmpty()) getFirebaseToken() else sdkSettings.firebaseToken })
            put("install_ts", sdkSettings.installTs)
            put("onscreen_cnt", sdkSettings.onscreenCnt)
            put("onscreen_sec", sdkSettings.onscreenSec)
            put("timezone", TimeZone.getDefault().rawOffset / 1000)
            put("lang", Locale.getDefault().language)
            put("country_net", getCountryCode())
            put("country_sim", getCountrySim().uppercase())
            put("ad_id", advId.orEmpty())
            put("ext_id", sdkSettings.extId)
            put("droid_api_ver",  Build.VERSION.SDK_INT)
            put("sdk_ver",  BuildConfig.VERSION_NAME)

            put("tags", JSONObject(sdkSettings.tags.toMap()))

            put("transport_type", "fcm.data")
            put("transport_token", sdkSettings.firebaseToken)

            put("platform_type", "android")
            put("platform_name", "android_api_${Build.VERSION.SDK_INT}")
        }
        return deviceConfig
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
}