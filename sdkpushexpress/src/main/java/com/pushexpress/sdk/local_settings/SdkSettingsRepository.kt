package com.pushexpress.sdk.local_settings

interface SdkSettingsRepository {

    suspend fun updateAppResumed()

    suspend fun updateAppStopped()

    suspend fun savePushExpressAppId(pushExpressAppId: String)

    suspend fun savePushExpressExternalId(externalId: String)

    suspend fun saveFirebaseToken(firebaseToken: String)

    suspend fun getSdkSettings(): SdkSettings

    suspend fun saveInstanceId(instanceId: String)

    suspend fun saveUuidv4(token: String)
    
    suspend fun getInstanceId(): String?

    suspend fun getUuidv4(): String? 

    suspend fun clearInstanceIdCache()

    suspend fun deleteInstanceId()

    suspend fun deleteExternalId()

    suspend fun deleteUuidv4() 

}
