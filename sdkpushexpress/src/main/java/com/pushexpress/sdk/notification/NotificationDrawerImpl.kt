package com.pushexpress.sdk.notification

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
import com.pushexpress.sdk.BuildConfig
import com.pushexpress.sdk.notification_actions.NotificationClickBroadcastReceiver

import com.pushexpress.sdk.main.SDK_TAG
import com.pushexpress.sdk.main.SdkPushExpress
import com.pushexpress.sdk.models.NotificationEvent

import com.pushexpress.sdk.notification_actions.TrampolineActivity
import com.pushexpress.sdk.utils.getBy
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.random.Random

internal class NotificationDrawerImpl (
    private val context: Context
) : com.pushexpress.sdk.notification.NotificationDrawer {
    override fun showNotification(data: Map<String, String>) {
        // It's ok for now to post new notification each time
        // (or we need to maintain global px.msg_id -> local notificationId map)
        val notificationId = Random.nextInt()

        val intent = Intent(
            context,
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
                context, notificationId, intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        } else {
            PendingIntent.getActivity(
                context, notificationId, intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        }

        val imageDisp = SdkPushExpress.imageLoader.enqueue(
            ImageRequest.Builder(context)
                .data(data[PX_IMAGE_KEY])
                .build()
        )
        val iconDisp = SdkPushExpress.imageLoader.enqueue(
            ImageRequest.Builder(context)
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

        val notificationBuilder = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(data.getOrElse(PX_TITLE_KEY) { "empty_title" })
            .setContentText(data.getOrElse(PX_BODY_KEY) { "empty_body" })
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            .setSmallIcon(androidx.loader.R.drawable.notification_icon_background)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setStyle(NotificationCompat.BigTextStyle())

        iconBmp?.let {notificationBuilder
            .setLargeIcon(iconBmp) }
        imageBmp?.let{notificationBuilder
            .setStyle(NotificationCompat.BigPictureStyle().bigPicture(imageBmp))}

        val notificationManager: NotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Since android Oreo notification channel is needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        SdkPushExpress.sdkApi.sendNotificationEvent(
            data[PX_MSG_ID_KEY] ?: "",
            NotificationEvent.DELIVERY
        )

        if (BuildConfig.LOG_DEBUG) Log.d(
            SDK_TAG,
            "Delivered: pxMsgId: " + data.getOrDefault(PX_MSG_ID_KEY) { "empty_id" } +
                    " notificationId: $notificationId")

        notificationManager.notify(notificationId, notificationBuilder.build())
    }

    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "sdkpushexpress_notification_channel"
        private const val NOTIFICATION_CHANNEL_NAME = "Default"
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