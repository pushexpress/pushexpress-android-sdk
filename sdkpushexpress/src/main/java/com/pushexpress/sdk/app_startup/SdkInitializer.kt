package com.pushexpress.sdk.app_startup

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.startup.Initializer
import coil.ImageLoader
import com.pushexpress.sdk.BuildConfig

import com.pushexpress.sdk.lifecycle.UILifecycleObserver
import com.pushexpress.sdk.local_settings.SdkSettingsRepositoryImpl
import com.pushexpress.sdk.main.SDK_TAG
import com.pushexpress.sdk.repository.ApiRepositoryImpl
import com.pushexpress.sdk.main.SdkPushExpress
import com.pushexpress.sdk.notification.NotificationDrawerImpl

internal class SdkInitializer : Initializer<Unit> {

    override fun create(context: Context) {
        if (BuildConfig.LOG_DEBUG) Log.d(SDK_TAG, "SDK Initialization")
        val settings = SdkSettingsRepositoryImpl(context)
        val imageLoader = ImageLoader(context)
        SdkPushExpress.sdkApi = ApiRepositoryImpl(context, settings)
        SdkPushExpress.sdkSettings = settings
        SdkPushExpress.imageLoader = imageLoader
        SdkPushExpress.notificationDrawer = NotificationDrawerImpl(context)

        (context as? Application)?.registerActivityLifecycleCallbacks(
            UILifecycleObserver(
                SdkPushExpress.sdkApi
            )
        )
    }

    override fun dependencies(): List<Class<out Initializer<*>>> {
        return emptyList()
    }
}