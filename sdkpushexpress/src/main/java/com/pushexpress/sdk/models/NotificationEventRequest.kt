package com.pushexpress.sdk.models

import androidx.annotation.Keep

@Keep
data class NotificationEventRequest(
    val app_id: String,
    val ic_token: String,
    val msg_id: String,
    val event: String
)
