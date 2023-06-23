package com.pushexpress.sdk.models

enum class EventsLifecycle(val event: String) {
    HBEAT("hbeat"),
    ONSCREEN("onscreen"),
    BACKGROUND("background"),
    // CLOSED("closed")
}