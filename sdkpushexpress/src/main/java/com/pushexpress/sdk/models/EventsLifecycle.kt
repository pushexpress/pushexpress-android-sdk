package com.pushexpress.sdk.models

import androidx.annotation.Keep

@Keep
enum class EventsLifecycle(val event: String) {
    HBEAT("hbeat"),
    ONSCREEN("onscreen"),
    BACKGROUND("background"),
    // CLOSED("closed")
}