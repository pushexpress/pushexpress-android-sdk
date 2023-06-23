package com.pushexpress.sdk.lifecycle

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.pushexpress.sdk.BuildConfig
import com.pushexpress.sdk.main.SDK_TAG
import com.pushexpress.sdk.local_settings.SdkSettingsRepository
import com.pushexpress.sdk.models.EventsLifecycle
import com.pushexpress.sdk.repository.ApiRepository
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.launch

class UILifecycleObserver(
    private val settingsRepository: SdkSettingsRepository,
    private val sdkApi: ApiRepository
) :
    Application.ActivityLifecycleCallbacks {

    private val handler = CoroutineExceptionHandler { _, exception ->
        println("$SDK_TAG: CoroutineExceptionHandler got $exception")
    }

    override fun onActivityCreated(activity: Activity, bundle: Bundle?) {
        // if (BuildConfig.LOG_DEBUG) Log.d(SDK_TAG, "Activity created")
    }

    override fun onActivityStarted(activity: Activity) {
        // if (BuildConfig.LOG_DEBUG) Log.d(SDK_TAG, "Activity started")
    }

    override fun onActivityPaused(activity: Activity) {
        // if (BuildConfig.LOG_DEBUG) Log.d(SDK_TAG, "Activity paused")
    }

    override fun onActivitySaveInstanceState(activity: Activity, bundle: Bundle) {
        // if (BuildConfig.LOG_DEBUG) Log.d(SDK_TAG, "Activity saved")
    }

    override fun onActivityDestroyed(activity: Activity) {
        // if (BuildConfig.LOG_DEBUG) Log.d(SDK_TAG, "Activity destroyed")
    }

    override fun onActivityResumed(activity: Activity) {
        if (BuildConfig.LOG_DEBUG) Log.d(SDK_TAG, "Activity resumed")

        (activity as? AppCompatActivity)?.let {
            it.lifecycleScope.launch(handler) {
                settingsRepository.updateAppResumed()
                sdkApi.sendLifecycleEvent(EventsLifecycle.ONSCREEN)
            }
        }
    }

    override fun onActivityStopped(activity: Activity) {
        if (BuildConfig.LOG_DEBUG) Log.d(SDK_TAG, "Activity stopped")

        (activity as? AppCompatActivity)?.let {
            it.lifecycleScope.launch(handler) {
                settingsRepository.updateAppStopped()
                sdkApi.sendLifecycleEvent(EventsLifecycle.BACKGROUND)
            }
        }
    }
}