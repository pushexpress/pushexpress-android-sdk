package com.pushexpress.sdk.models

data class NotificationEventRequest(
    val app_id: String,
    val ic_token: String,
    val msg_id: String,
    val event: String
)
