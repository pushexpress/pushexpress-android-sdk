package com.pushexpress.sdk.models

import androidx.annotation.Keep

@Keep
data class DeviceConfigResponse(val device_intvl: Long, val hbeat_intvl: Long)