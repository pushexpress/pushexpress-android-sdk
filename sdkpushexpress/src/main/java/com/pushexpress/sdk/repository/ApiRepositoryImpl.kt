package com.pushexpress.sdk.repository

import android.content.Context
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
        println("SdkPushExpress: CoroutineExceptionHandler got $exception")
    }
    private val scope = CoroutineScope(Dispatchers.IO + commonJob + handler)

    override suspend fun doApiLoop() =
        withContext(scope.coroutineContext) {
            Log.d(TAG, "doApiLoop")

            devicesJob.cancel()
            heartBeatsJob.cancel()
            try {
                val res =
                    retryHttpIO(times = 10) { sdkService.sendDeviceConfig(createDevicesRequest()) }
                repeatRequestDevices(res.device_intvl)
                repeatRequestHeartBeat(res.hbeat_intvl)
            } catch (e: Exception) {
                Log.d(TAG, "doApiLoop: unhandled error: $e")
                // repeat loop now
                repeatRequestDevices(1)
            }
        }

    // send only once after appId or ExtId changes
    override suspend fun sendDeviceConfig() =
        withContext(scope.coroutineContext) {
            Log.d(TAG, "sendDeviceConfig")
            sdkService.sendDeviceConfig(createDevicesRequest())
        }

    override suspend fun sendLifecycleEvent(event: EventsLifecycle) {
        scope.launch {
            val settings = settingsRepository.getSdkSettings()
            Log.d(TAG, "sendLifecycleEvent ${event.event}")
            sdkService.sendLifecycleEvent(
                EventsLifecycleRequest(
                    app_id = settings.appId,
                    ic_token = settings.instanceToken,
                    event = event.event
                )
            )
        }
    }

    override fun sendNotificationEvent(messageId: String, event: NotificationEvent) {
        scope.launch {
            Log.d(TAG, "sendNotificationEvent")
            val sdkSettings = settingsRepository.getSdkSettings()
            sdkService.sendNotificationEvent(
                NotificationEventRequest(
                    app_id = sdkSettings.appId,
                    ic_token = sdkSettings.instanceToken,
                    event = event.event,
                    msg_id = messageId
                )
            )
        }
    }

    private fun repeatRequestDevices(timeSec: Long) {
        devicesJob = scope.launch {
            Log.d(TAG, "repeatRequestDevices")
            timeSec.let {
                delay(it * 1000)
                ensureActive()
                doApiLoop()
            }
        }
    }

    private fun repeatRequestHeartBeat(timeSec: Long) {
        heartBeatsJob = scope.launch {
            Log.d(TAG, "repeatRequestHeartBeat")
            timeSec.let {
                while (isActive) {
                    delay(it * 1000)
                    ensureActive()
                    sendLifecycleEvent(EventsLifecycle.HBEAT)
                }
            }
        }
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
            fcm_token = sdkSettings.firebaseToken ?: getFirebaseToken(),
            ad_id = advId.orEmpty(),
            onscreen_cnt = sdkSettings.onscreenCnt,
            onscreen_sec = sdkSettings.onscreenSec
        )
        Log.d(TAG, "deviceConfig: $dc")
        return dc
    }

    private suspend fun getFirebaseToken(): String {
        return suspendCoroutine { continuation ->
            Firebase.messaging.token.addOnCompleteListener {
                if (it.isSuccessful) {
                    Log.d(TAG, "Fetched FCM registration: token=${it.result}")
                    settingsRepository.saveFirebaseToken(it.result)
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
        private const val TAG = "SdkPushExpress"
    }
}