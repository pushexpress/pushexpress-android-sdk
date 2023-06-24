package com.pushexpress.sdk.repository

import android.content.Context
import android.os.Build
import android.telephony.TelephonyManager
import android.util.Log
import com.google.android.gms.ads.identifier.AdvertisingIdClient.getAdvertisingIdInfo

import com.pushexpress.sdk.local_settings.SdkSettingsRepository
import com.pushexpress.sdk.retrofit.RetrofitBuilder
import com.pushexpress.sdk.utils.retryHttpIO
import kotlinx.coroutines.*
import java.util.*

import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.ktx.messaging
import com.pushexpress.sdk.BuildConfig
import com.pushexpress.sdk.main.SDK_TAG
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
            if (BuildConfig.LOG_DEBUG) Log.d(SDK_TAG, "doApiLoop")

            devicesJob.cancel()
            heartBeatsJob.cancel()
            try {
                val res = retryHttpIO(times = 10) { createAndSendDeviceConfig() }
                repeatRequestDevices(res.device_intvl)
                repeatRequestHeartBeat(res.hbeat_intvl)
            } catch (e: Exception) {
                if (BuildConfig.LOG_RELEASE) Log.d(SDK_TAG, "doApiLoop: unhandled error: $e")
                // repeat loop now
                repeatRequestDevices(1)
            }
        }

    // send only once after appId or ExtId changes
    override suspend fun sendDeviceConfig() =
        withContext(scope.coroutineContext) {
            createAndSendDeviceConfig()
        }

    override suspend fun sendLifecycleEvent(event: EventsLifecycle) {
        scope.launch {
            if (BuildConfig.LOG_DEBUG) Log.d(SDK_TAG, "sendLifecycleEvent[${event.event}]")

            val settings = settingsRepository.getSdkSettings()
            val evt = EventsLifecycleRequest(
                app_id = settings.appId,
                ic_token = settings.instanceToken,
                event = event.event
            )
            if (BuildConfig.LOG_RELEASE) Log.d(SDK_TAG,
                "sendLifecycleEvent[${event.event}]: ${evt}")
            sdkService.sendLifecycleEvent(evt)
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

            val sdkSettings = settingsRepository.getSdkSettings()
            val evt = NotificationEventRequest(
                app_id = sdkSettings.appId,
                ic_token = sdkSettings.instanceToken,
                event = event.event,
                msg_id = messageId
            )
            if (BuildConfig.LOG_RELEASE) Log.d(SDK_TAG,
                "sendNotificationEvent[$messageId, ${event.event}]: ${evt}")
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
        val dc = createDevicesRequest()
        if (BuildConfig.LOG_RELEASE) Log.d(SDK_TAG, "sendDeviceConfig: $dc")
        return sdkService.sendDeviceConfig(dc)
    }

    private suspend fun createDevicesRequest(): DeviceConfigRequest {
        val advId = try {
            getAdvertisingIdInfo(context).id.orEmpty()
        } catch (e: Exception) {
            null
        }
        val sdkSettings = settingsRepository.getSdkSettings()

        val dc = DeviceConfigRequest(
            app_id = sdkSettings.appId,
            ic_token = sdkSettings.instanceToken,
            ext_id = sdkSettings.extId,
            lang = Locale.getDefault().language,
            country_net = getCountryCode(),
            country_sim = getCountrySim().uppercase(),
            timezone = TimeZone.getDefault().rawOffset / 1000,
            install_ts = sdkSettings.installTs,
            fcm_token = sdkSettings.firebaseToken.let {
                if (it.isEmpty()) getFirebaseToken() else sdkSettings.firebaseToken },
            ad_id = advId.orEmpty(),
            onscreen_cnt = sdkSettings.onscreenCnt,
            onscreen_sec = sdkSettings.onscreenSec,
            droid_api_ver = Build.VERSION.SDK_INT,
        )
        return dc
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
        private const val SDK_PUSHEXPRESS_COMMON_URL = "https://sdk.push.express/r/v1/"
    }
}