package com.pushexpress.sdk.notification_actions


import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.pushexpress.sdk.BuildConfig
import com.pushexpress.sdk.main.SDK_TAG
import com.pushexpress.sdk.main.SdkPushExpress
import com.pushexpress.sdk.models.NotificationEvent
import com.pushexpress.sdk.notification.NotificationDrawerImpl.Companion.EXTRA_PX_MSG_ID

class TrampolineActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val extras = intent?.extras
        val pxMsgId = extras?.getString(EXTRA_PX_MSG_ID).orEmpty()
        SdkPushExpress.sdkApi.sendNotificationEvent(pxMsgId, NotificationEvent.CLICK)

        startDefaultActivity(this.applicationContext)
        // TODO: define clean openlink workflow
        // startOpenLinkIntent(this, extras?.getString(FcmService.EXTRA_PX_LINK))

        if (BuildConfig.LOG_DEBUG) Log.d(SDK_TAG, "Clicked: $pxMsgId")
        finish()
    }
}