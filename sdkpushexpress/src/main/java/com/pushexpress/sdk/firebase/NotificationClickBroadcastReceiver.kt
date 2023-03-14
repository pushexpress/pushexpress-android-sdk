package com.pushexpress.sdk.firebase

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import com.pushexpress.sdk.common.startDefaultActivity
import com.pushexpress.sdk.common.startOpenLinkIntent
import com.pushexpress.sdk.firebase.FirebaseMessagingService.Companion.EXTRA_PX_LINK
import com.pushexpress.sdk.firebase.FirebaseMessagingService.Companion.EXTRA_PX_MSG_ID
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
        Log.d(TAG, "Clicked: $pxMsgId")
    }

    override fun peekService(myContext: Context?, service: Intent?): IBinder {
        return super.peekService(myContext, service)
    }

    companion object {
        private const val TAG = "SdkPushExpress"
    }
}