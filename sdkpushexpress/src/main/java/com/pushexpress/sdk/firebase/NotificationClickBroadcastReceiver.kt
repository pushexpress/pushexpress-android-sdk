package com.pushexpress.sdk.firebase

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.util.Log
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
        } else {
            startOpenLinkIntent(context, intent?.getStringExtra(EXTRA_PX_LINK))
        }
        Log.d(TAG, "Clicked: $pxMsgId")
    }

    private fun startDefaultActivity(context: Context?){
        context?.let {
                ctx ->
            val packageManager = ctx.packageManager
            val activityIntent = packageManager.getLaunchIntentForPackage(context.packageName)
            activityIntent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            context.startActivity(activityIntent)
        }
    }

    private fun startOpenLinkIntent(context: Context?, link: String?) {
        link?.let {
            val viewIntent = Intent(Intent.ACTION_VIEW)
            try {
                viewIntent.data = Uri.parse(link)
                viewIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                context?.startActivity(viewIntent)
            } catch (e: Exception) {
                Log.d(TAG, "Can't open link: $e")
            }
        }
    }

    override fun peekService(myContext: Context?, service: Intent?): IBinder {
        return super.peekService(myContext, service)
    }

    companion object {
        private const val TAG = "SdkPushExpress"
    }
}