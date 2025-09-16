package com.pushexpress.sdk.models

data class DeviceConfigResponse(val device_intvl: Long, val hbeat_intvl: Long)

data class RegisterInstanceResponse(
    val id: String,
    val just_created: Boolean,
    val ic_token: String? = null
)