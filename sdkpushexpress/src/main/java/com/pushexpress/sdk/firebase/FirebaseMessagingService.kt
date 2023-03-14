package com.pushexpress.sdk.firebase

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.toBitmap

import coil.request.ImageRequest
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.pushexpress.sdk.R
import com.pushexpress.sdk.common.SDK_TAG
import com.pushexpress.sdk.main.SdkPushExpress
import com.pushexpress.sdk.models.NotificationEvent
import com.pushexpress.sdk.trampoline_activity.TrampolineActivity
import com.pushexpress.sdk.utils.getBy
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.random.Random

open class FirebaseMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        SdkPushExpress.sdkSettings.saveFirebaseToken(token)
        Log.d(SDK_TAG, "onNewToken: token=${token}")
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(SDK_TAG, "From: ${remoteMessage.from}")

        // Check if message contains a data payload.
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(SDK_TAG, "Message data payload: ${remoteMessage.data}")
        }

        sendNotification(remoteMessage.data)
    }

    private fun sendNotification(data: Map<String, String>) {
        // It's ok for now to post new notification each time
        // (or we need to maintain global px.msg_id -> local notificationId map)
        val notificationId = Random.nextInt()

        val intent = Intent(
            applicationContext,
            getBy(
                Build.VERSION.SDK_INT < Build.VERSION_CODES.S,
                NotificationClickBroadcastReceiver::class.java,
                TrampolineActivity::class.java
            )
        ).also { intent ->
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            intent.action = INTENT_ACTION_CLICK
            data[PX_MSG_ID_KEY]?.let { intent.putExtra(EXTRA_PX_MSG_ID, it) }
            data[PX_LINK_KEY]?.let { intent.putExtra(EXTRA_PX_LINK, it) }
        }

        val pendingIntent = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            PendingIntent.getBroadcast(
                this.applicationContext, notificationId, intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        } else {
            PendingIntent.getActivity(
                this.applicationContext, notificationId, intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        }

        val imageDisp = SdkPushExpress.imageLoader.enqueue(
            ImageRequest.Builder(this)
                .data(data[PX_IMAGE_KEY])
                .build()
        )
        val iconDisp = SdkPushExpress.imageLoader.enqueue(
            ImageRequest.Builder(this)
                .data(data[PX_ICON_KEY])
                .build()
        )

        var imageBmp: Bitmap? = null
        var iconBmp: Bitmap? = null
        // TODO: timeout in config
        runBlocking {
            withTimeoutOrNull(3000) {
                launch {
                    imageBmp = imageDisp.job.await().drawable?.toBitmap()
                    iconBmp = iconDisp.job.await().drawable?.toBitmap()
                }
            }
        }

        val channelId = getString(R.string.default_notification_channel_name)
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setContentTitle(data.getOrElse(PX_TITLE_KEY) { "empty_title" })
            .setContentText(data.getOrElse(PX_BODY_KEY) { "empty_body" })
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            .setSmallIcon(androidx.loader.R.drawable.notification_icon_background)
            .setLargeIcon(iconBmp)
            .setStyle(NotificationCompat.BigPictureStyle().bigPicture(imageBmp))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Since android Oreo notification channel is needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                getString(R.string.default_notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        SdkPushExpress.sdkApi.sendNotificationEvent(
            data[PX_MSG_ID_KEY] ?: "",
            NotificationEvent.DELIVERY
        )

        Log.d(
            SDK_TAG,
            "Delivered: pxMsgId: " + data.getOrDefault(PX_MSG_ID_KEY) { "empty_id" } +
                    " notificationId: $notificationId")

        notificationManager.notify(notificationId, notificationBuilder.build())
    }

    companion object {
        private const val INTENT_ACTION_CLICK = "com.pushexpress.sdk.ACTION_CLICK"
        private const val PX_TITLE_KEY = "px.title"
        private const val PX_BODY_KEY = "px.body"
        private const val PX_MSG_ID_KEY = "px.msg_id"
        private const val PX_IMAGE_KEY = "px.image"
        private const val PX_ICON_KEY = "px.icon"
        private const val PX_LINK_KEY = "px.link"
        const val EXTRA_PX_MSG_ID = "EXTRA_PX_MSG_ID"
        const val EXTRA_PX_LINK = "EXTRA_PX_LINK"
    }
}