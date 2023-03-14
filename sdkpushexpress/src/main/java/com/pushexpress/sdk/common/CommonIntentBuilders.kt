package com.pushexpress.sdk.common

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log

const val SDK_TAG = "SdkPushExpress"

fun startDefaultActivity(context: Context?) {
    context?.let { ctx ->
        val packageManager = ctx.packageManager
        val activityIntent = packageManager.getLaunchIntentForPackage(context.packageName)
        activityIntent?.addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        )
        context.startActivity(activityIntent)
    }
}

fun startOpenLinkIntent(context: Context?, link: String?) {
    link?.let {
        val viewIntent = Intent(Intent.ACTION_VIEW)
        try {
            viewIntent.data = Uri.parse(link)
            viewIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            context?.startActivity(viewIntent)
        } catch (e: Exception) {
            Log.d(SDK_TAG, "Can't open link: $e")
        }
    }
}