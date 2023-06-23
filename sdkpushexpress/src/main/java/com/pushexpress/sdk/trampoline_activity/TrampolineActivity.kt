package com.pushexpress.sdk.trampoline_activity


import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.pushexpress.sdk.BuildConfig
import com.pushexpress.sdk.main.SDK_TAG
import com.pushexpress.sdk.lifecycle.startDefaultActivity
import com.pushexpress.sdk.fcm.FcmService
import com.pushexpress.sdk.main.SdkPushExpress
import com.pushexpress.sdk.models.NotificationEvent

class TrampolineActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val extras = intent?.extras
        val pxMsgId = extras?.getString(FcmService.EXTRA_PX_MSG_ID).orEmpty()
        SdkPushExpress.sdkApi.sendNotificationEvent(pxMsgId, NotificationEvent.CLICK)

        startDefaultActivity(this.applicationContext)
        // TODO: define clean openlink workflow
        // startOpenLinkIntent(this, extras?.getString(FcmService.EXTRA_PX_LINK))

        if (BuildConfig.LOG_DEBUG) Log.d(SDK_TAG, "Clicked: $pxMsgId")
        finish()
    }
}