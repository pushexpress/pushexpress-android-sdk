package com.pushexpress.sdk.local_settings

interface SdkSettingsRepository {

    suspend fun updateAppResumed()

    suspend fun updateAppStopped()

    suspend fun setTag(tagKey: String, tagValue: String)

    suspend fun saveDeviceInstanceId(instanceId: String)

    suspend fun savePushExpressAppId(pushExpressAppId: String)

    suspend fun savePushExpressExternalId(externalId: String)

    suspend fun saveFirebaseToken(firebaseToken: String)

    suspend fun getSdkSettings(): SdkSettings
}
