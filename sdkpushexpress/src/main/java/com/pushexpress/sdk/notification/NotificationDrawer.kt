package com.pushexpress.sdk.notification

internal interface NotificationDrawer {
    fun showNotification(data: Map<String, String>)
}