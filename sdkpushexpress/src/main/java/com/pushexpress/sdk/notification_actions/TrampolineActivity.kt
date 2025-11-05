package com.pushexpress.sdk.notification_actions


import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.pushexpress.sdk.BuildConfig
import com.pushexpress.sdk.main.SDK_TAG
import com.pushexpress.sdk.main.SdkPushExpress
import com.pushexpress.sdk.models.NotificationEvent
import com.pushexpress.sdk.notification.NotificationDrawerImpl.Companion.EXTRA_PX_MSG_ID
import com.pushexpress.sdk.notification.NotificationDrawerImpl.Companion.EXTRA_PX_LINK
import com.pushexpress.sdk.notification_actions.startDefaultActivity
class TrampolineActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val extras = intent?.extras
        val pxMsgId = extras?.getString(EXTRA_PX_MSG_ID).orEmpty()
        val deepLink = extras?.getString(EXTRA_PX_LINK)

        SdkPushExpress.sdkApi.sendNotificationEvent(pxMsgId, NotificationEvent.CLICK)

        if (!deepLink.isNullOrEmpty()) {
            startDeepLinkIntent(this, deepLink)
        } else {
            startDefaultActivity(this.applicationContext)
        }

        if (BuildConfig.LOG_DEBUG) Log.d(SDK_TAG, "Clicked: $pxMsgId")
        finish()
    }

    private fun startDeepLinkIntent(context: Context, deepLink: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(deepLink))
        intent.setPackage(context.packageName)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        context.startActivity(intent)
    }
}