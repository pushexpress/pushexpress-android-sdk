package com.pushexpress.sdk.models

data class DeviceConfigRequest(
    val app_id: String,
    val ic_token: String,
    val ext_id: String,
    val fcm_token: String,
    val lang: String,
    val ad_id: String,
    val country_net: String,
    val country_sim: String,
    val timezone: Int,
    val install_ts: Long,
    val onscreen_cnt: Int,
    val onscreen_sec: Long,
    val droid_api_ver: Int,
)
