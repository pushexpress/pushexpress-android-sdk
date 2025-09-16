package com.pushexpress.sdk.models

enum class NotificationEvent(val event: String) {
    DELIVERY("delivered"),
    CLICK("clicked")
}