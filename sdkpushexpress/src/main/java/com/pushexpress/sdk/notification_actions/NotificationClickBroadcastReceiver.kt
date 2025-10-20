package com.pushexpress.sdk.notification_actions

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.util.Log
import com.pushexpress.sdk.BuildConfig
import com.pushexpress.sdk.main.SDK_TAG
import com.pushexpress.sdk.notification.NotificationDrawerImpl.Companion.EXTRA_PX_MSG_ID
import com.pushexpress.sdk.notification.NotificationDrawerImpl.Companion.EXTRA_PX_LINK
import com.pushexpress.sdk.main.SdkPushExpress
import com.pushexpress.sdk.models.NotificationEvent

class NotificationClickBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) {
            Log.e(SDK_TAG, "Context or Intent is null")
            return
        }
        val pxMsgId = intent?.getStringExtra(EXTRA_PX_MSG_ID) ?: ""
        val deepLink = intent.getStringExtra(EXTRA_PX_LINK)

        SdkPushExpress.sdkApi.sendNotificationEvent(pxMsgId, NotificationEvent.CLICK)

        if (!deepLink.isNullOrEmpty()) {
            startDeepLinkIntent(context, deepLink)
        } else {
            startDefaultActivity(context)
        }

        if (BuildConfig.LOG_DEBUG) Log.d(SDK_TAG, "Clicked: $pxMsgId")
    }

    override fun peekService(myContext: Context?, service: Intent?): IBinder {
        return super.peekService(myContext, service)
    }

    private fun startDeepLinkIntent(context: Context, deepLink: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(deepLink))
        intent.setPackage(context.packageName)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        context.startActivity(intent)
    }
}