package com.pushexpress.sdk.trampoline_activity


import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.pushexpress.sdk.common.SDK_TAG
import com.pushexpress.sdk.common.startDefaultActivity
import com.pushexpress.sdk.common.startOpenLinkIntent
import com.pushexpress.sdk.firebase.FirebaseMessagingService
import com.pushexpress.sdk.main.SdkPushExpress
import com.pushexpress.sdk.models.NotificationEvent

class TrampolineActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val extras = intent?.extras
        val pxMsgId = extras?.getString(FirebaseMessagingService.EXTRA_PX_MSG_ID).orEmpty()
        SdkPushExpress.sdkApi.sendNotificationEvent(pxMsgId, NotificationEvent.CLICK)

        startDefaultActivity(this.applicationContext)
        // TODO: define clean openlink workflow
        // startOpenLinkIntent(this, extras?.getString(FirebaseMessagingService.EXTRA_PX_LINK))

        Log.d(SDK_TAG, "Clicked: $pxMsgId")
        finish()
    }
}