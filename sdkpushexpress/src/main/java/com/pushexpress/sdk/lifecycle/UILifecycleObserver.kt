package com.pushexpress.sdk.lifecycle

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.util.Log
import com.pushexpress.sdk.BuildConfig
import com.pushexpress.sdk.main.SDK_TAG
import com.pushexpress.sdk.models.EventsLifecycle
import com.pushexpress.sdk.repository.ApiRepository
import kotlinx.coroutines.CoroutineExceptionHandler

class UILifecycleObserver(
    private val sdkApi: ApiRepository
) :
    Application.ActivityLifecycleCallbacks {

    private val handler = CoroutineExceptionHandler { _, exception ->
        println("$SDK_TAG: CoroutineExceptionHandler got $exception")
    }

    override fun onActivityCreated(activity: Activity, bundle: Bundle?) {
        // if (BuildConfig.LOG_DEBUG) Log.d(SDK_TAG, "onActivityCreated")
    }

    override fun onActivityStarted(activity: Activity) {
        // if (BuildConfig.LOG_DEBUG) Log.d(SDK_TAG, "onActivityStarted")
    }

    override fun onActivityPaused(activity: Activity) {
        // if (BuildConfig.LOG_DEBUG) Log.d(SDK_TAG, "onActivityPaused")
    }

    override fun onActivitySaveInstanceState(activity: Activity, bundle: Bundle) {
        // if (BuildConfig.LOG_DEBUG) Log.d(SDK_TAG, "onActivitySaveInstanceState")
    }

    override fun onActivityDestroyed(activity: Activity) {
        // if (BuildConfig.LOG_DEBUG) Log.d(SDK_TAG, "onActivityDestroyed")
    }

    override fun onActivityResumed(activity: Activity) {
        if (BuildConfig.LOG_DEBUG) Log.d(SDK_TAG, "onActivityResumed")
        sdkApi.sendLifecycleEvent(EventsLifecycle.ONSCREEN)
    }

    override fun onActivityStopped(activity: Activity) {
        if (BuildConfig.LOG_DEBUG) Log.d(SDK_TAG, "onActivityStopped")
        sdkApi.sendLifecycleEvent(EventsLifecycle.BACKGROUND)
    }
}