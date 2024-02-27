package com.pushexpress.sdk.local_settings

data class SdkSettings(
    val instanceToken: String,
    val instanceId: String,
    val installTs: Long,
    val appId: String,
    val extId: String,
    val firebaseToken: String,
    val onscreenCnt: Int,
    val onscreenSec: Long,
    val resumedTs: Long,
    val stoppedTs: Long
)