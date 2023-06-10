package com.pushexpress.sdk.models

import androidx.annotation.Keep

@Keep
enum class NotificationEvent(val event: String) {
    DELIVERY("delivery"),
    CLICK("click")
}