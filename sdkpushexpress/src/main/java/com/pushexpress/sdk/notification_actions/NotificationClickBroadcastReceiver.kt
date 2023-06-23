package com.pushexpress.sdk.notification_actions

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import com.pushexpress.sdk.BuildConfig
import com.pushexpress.sdk.main.SDK_TAG
import com.pushexpress.sdk.notification.NotificationDrawerImpl.Companion.EXTRA_PX_MSG_ID
import com.pushexpress.sdk.main.SdkPushExpress
import com.pushexpress.sdk.models.NotificationEvent

class NotificationClickBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val pxMsgId = intent?.getStringExtra(EXTRA_PX_MSG_ID) ?: ""
        SdkPushExpress.sdkApi.sendNotificationEvent(pxMsgId, NotificationEvent.CLICK)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            startDefaultActivity(context)
        }
        /* TODO: define clean openlink workflow
        else {
            startOpenLinkIntent(context, intent?.getStringExtra(EXTRA_PX_LINK))
        }*/
        if (BuildConfig.LOG_DEBUG) Log.d(SDK_TAG, "Clicked: $pxMsgId")
    }

    override fun peekService(myContext: Context?, service: Intent?): IBinder {
        return super.peekService(myContext, service)
    }
}