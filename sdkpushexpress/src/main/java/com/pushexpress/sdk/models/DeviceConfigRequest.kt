package com.pushexpress.sdk.models

data class RegisterInstanceRequest(
    val ic_token: String?,
    val ext_id: String?
)

data class DeviceInfoRequest(
    val transport_type: String,
    val transport_token: String,
    val platform_type: String,
    val lang: String,
    val agent_name: String,
    val tz_sec: Int,
    val notif_perm_granted: Boolean,
    val tags: DeviceTags
)

data class DeviceTags(
    val adID: String,
    val segment: String,
    val webmaster: String
)