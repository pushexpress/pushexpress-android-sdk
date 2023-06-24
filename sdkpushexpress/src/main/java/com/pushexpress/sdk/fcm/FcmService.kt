package com.pushexpress.sdk.fcm

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

import com.pushexpress.sdk.BuildConfig
import com.pushexpress.sdk.main.SDK_TAG
import com.pushexpress.sdk.main.SdkPushExpress

open class FcmService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        super.onNewToken(token)

        if (BuildConfig.LOG_DEBUG) Log.d(SDK_TAG, "onNewToken: token=${token}")
        SdkPushExpress.sdkApi.saveFirebaseToken(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        if (BuildConfig.LOG_DEBUG) Log.d(SDK_TAG,
            "onMessageReceived: from=${remoteMessage.from}")

        // Check if message contains a data payload.
        if (remoteMessage.data.isNotEmpty()) {
            if (BuildConfig.LOG_DEBUG) Log.d(SDK_TAG,
                "onMessageReceived: data=${remoteMessage.data}")
        }

        SdkPushExpress.notificationDrawer.showNotification(remoteMessage.data)
    }
}
