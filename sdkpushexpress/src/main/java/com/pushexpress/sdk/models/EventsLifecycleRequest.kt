package com.pushexpress.sdk.models

import androidx.annotation.Keep

@Keep
data class EventsLifecycleRequest(
    val app_id: String,
    val ic_token: String,
    val event: String
)
