package com.pushexpress.sdk.models

data class EventsLifecycleRequest(
    val app_id: String,
    val ic_token: String,
    val event: String
)
