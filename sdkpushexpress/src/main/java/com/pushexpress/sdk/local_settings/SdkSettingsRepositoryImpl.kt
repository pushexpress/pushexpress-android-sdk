package com.pushexpress.sdk.local_settings

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.pushexpress.sdk.BuildConfig
import com.pushexpress.sdk.main.SDK_TAG
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.util.*

class SdkSettingsRepositoryImpl(private val context: Context) : SdkSettingsRepository {

    private val Context.dataStore: DataStore<Preferences> by
                                preferencesDataStore(name = STORAGE_NAME)
    private val instanceToken = stringPreferencesKey(INSTANCE_TOKEN)
    private val installTs = longPreferencesKey(INSTALL_TS)
    private val appId = stringPreferencesKey(APP_ID)
    private val extId = stringPreferencesKey(EXT_ID)
    @Volatile
    private var firebaseToken: String = ""
    private val onscreenCnt = intPreferencesKey(ONSCREEN_CNT)
    private val onscreenSec = longPreferencesKey(ONSCREEN_SEC)
    private val resumedTs = longPreferencesKey(RESUMED_TS)
    private val stoppedTs = longPreferencesKey(STOPPED_TS)
    private val instanceIdKey = stringPreferencesKey(KEY_INSTANCE_ID)
    private val uuidv4Key = stringPreferencesKey("uuidv4_token")
    @Volatile
    private var cachedUuidv4: String? = null

    @Volatile
    private var cachedInstanceId: String? = null

    init {
        runBlocking {
            context.dataStore.edit { settings ->
                if (settings[instanceToken] == null) {
                    zeroSettings(settings)
                }
            }
            preloadInstanceId()
        }
    }

    private suspend fun preloadInstanceId() {
        try {
            cachedInstanceId = context.dataStore.data
                .map { preferences -> preferences[instanceIdKey] }
                .first()
            if (BuildConfig.LOG_DEBUG) Log.d(SDK_TAG, "Preloaded instanceId: $cachedInstanceId")
        } catch (e: Exception) {
            Log.e(SDK_TAG, "Failed to preload instanceId", e)
        }
    }

    override suspend fun saveFirebaseToken(firebaseToken: String) {
        this.firebaseToken = firebaseToken
    }

    override suspend fun savePushExpressExternalId(externalId: String) {
        context.dataStore.edit { settings ->
            settings[this.extId] = externalId
        }
    }

    override suspend fun saveUuidv4(token: String) {
        context.dataStore.edit { preferences ->
            preferences[uuidv4Key] = token
        }
        cachedUuidv4 = token
        if (BuildConfig.LOG_DEBUG) Log.d(SDK_TAG, "UUIDv4 saved: $token")
    }

    override suspend fun getUuidv4(): String? { 
        if (cachedUuidv4 != null) {
            return cachedUuidv4
        }
        
        return try {
            val token = context.dataStore.data
                .map { preferences -> preferences[uuidv4Key] }
                .first()
            cachedUuidv4 = token
            token
        } catch (e: Exception) {
            Log.e(SDK_TAG, "Failed to get UUIDv4", e)
            null
        }
    }

    override suspend fun deleteUuidv4() {
        context.dataStore.edit { preferences ->
            preferences.remove(uuidv4Key)
        }
        cachedUuidv4 = null
        if (BuildConfig.LOG_DEBUG) Log.d(SDK_TAG, "UUIDv4 deleted")
    }

    override suspend fun savePushExpressAppId(pushExpressAppId: String) {
        context.dataStore.edit { settings ->
            if (settings[instanceToken].orEmpty().isEmpty() ||
                settings[this.appId] != pushExpressAppId
            ) {
                zeroSettings(settings)
                genNewInstall(settings, pushExpressAppId)
                if (BuildConfig.LOG_RELEASE) Log.d(SDK_TAG,
                    "Generate install with new appId: $pushExpressAppId")
            } else {
                if (BuildConfig.LOG_RELEASE) Log.d(SDK_TAG,
                    "Got appId with existing install: $pushExpressAppId")
            }
        }
    }

    override suspend fun saveInstanceId(instanceId: String) {
        context.dataStore.edit { settings ->
            settings[instanceIdKey] = instanceId
        }
        cachedInstanceId = instanceId
        if (BuildConfig.LOG_DEBUG) Log.d(SDK_TAG, "InstanceId saved to cache: $instanceId")
    }

    override suspend fun getInstanceId(): String? {
        if (cachedInstanceId != null) {
            if (BuildConfig.LOG_DEBUG) Log.d(SDK_TAG, "InstanceId from cache: $cachedInstanceId")
            return cachedInstanceId
        }
        
        return try {
            val instanceId = context.dataStore.data
                .map { preferences -> preferences[instanceIdKey] }
                .first()
            cachedInstanceId = instanceId
            if (BuildConfig.LOG_DEBUG) Log.d(SDK_TAG, "InstanceId loaded from DataStore: $instanceId")
            instanceId
        } catch (e: Exception) {
            Log.e(SDK_TAG, "Failed to get instanceId from DataStore", e)
            null
        }
    }

    override suspend fun clearInstanceIdCache() {
        cachedInstanceId = null
        deleteInstanceId()
        if (BuildConfig.LOG_DEBUG) Log.d(SDK_TAG, "InstanceId cache cleared")
    }

    override suspend fun deleteInstanceId() {
        try {
            context.dataStore.edit { preferences ->
                preferences.remove(instanceIdKey)
            }
            cachedInstanceId = null
            if (BuildConfig.LOG_DEBUG) Log.d(SDK_TAG, "✅ InstanceId completely deleted")
        } catch (e: Exception) {
            Log.e(SDK_TAG, "❌ Failed to delete instanceId", e)
            throw e
        }
    }

    override suspend fun deleteExternalId() {
        try {
            context.dataStore.edit { preferences ->
                preferences.remove(extId)
            }
            if (BuildConfig.LOG_DEBUG) Log.d(SDK_TAG, "✅ extId completely deleted")
        } catch (e: Exception) {
            Log.e(SDK_TAG, "❌ Failed to delete extId", e)
            throw e
        }
    }

    fun isInstanceIdCached(): Boolean = cachedInstanceId != null

    override suspend fun updateAppResumed() {
        context.dataStore.edit { settings ->
            settings[onscreenCnt] = (settings[onscreenCnt] ?: 0) + 1
            settings[resumedTs] = System.currentTimeMillis() / 1000

            if (BuildConfig.LOG_DEBUG) Log.d(SDK_TAG, "appResumed: ${settings[resumedTs]} " +
                    "${settings[onscreenCnt]} ${settings[onscreenSec] ?: 0}")
        }
    }

    override suspend fun updateAppStopped() {
        context.dataStore.edit { settings ->
            val nowTs = System.currentTimeMillis() / 1000
            val lastResumedTs = settings[resumedTs] ?: nowTs
            val fgSec = (nowTs - lastResumedTs).coerceAtLeast(0)

            settings[onscreenSec] = (settings[onscreenSec] ?:0) + fgSec
            settings[stoppedTs] = nowTs

            if (BuildConfig.LOG_DEBUG) Log.d(SDK_TAG, "appStopped: ${settings[stoppedTs]} ${settings[onscreenCnt]} ${settings[onscreenSec]}")
        }
    }

    override suspend fun getSdkSettings(): SdkSettings {
        return context.dataStore.data.map {
            SdkSettings(
                it[instanceToken].orEmpty(),
                it[installTs] ?: 0,
                it[appId].orEmpty(),
                it[extId].orEmpty(),
                firebaseToken,
                it[onscreenCnt] ?: 0,
                it[onscreenSec] ?: 0,
                it[resumedTs] ?: 0,
                it[stoppedTs] ?: 0,
            )
        }.first()
    }

    private fun zeroSettings(settings: MutablePreferences) {
        settings.apply {
            this[instanceToken] = ""
            this[installTs] = 0
            this[appId] = ""
            this[extId] = ""
            this[onscreenCnt] = 0
            this[onscreenSec] = 0
            this[resumedTs] = 0
            this[stoppedTs] = 0
            this[instanceIdKey] = ""
        }
        cachedInstanceId = null
    }

    private fun genNewInstall(settings: MutablePreferences, pxAppId: String) {
        settings.apply {
            this[instanceToken] = UUID.randomUUID().toString()
            this[installTs] = System.currentTimeMillis() / 1000
            this[appId] = pxAppId
        }
    }

    companion object {
        private const val STORAGE_NAME = "sdkpushexpress"
        private const val INSTANCE_TOKEN = "ic_token"
        private const val INSTALL_TS = "install_ts"
        private const val APP_ID = "app_id"
        private const val EXT_ID = "ext_id"
        private const val ONSCREEN_CNT = "onscreen_cnt"
        private const val ONSCREEN_SEC = "onscreen_sec"
        private const val RESUMED_TS = "resumed_ts"
        private const val STOPPED_TS = "stopped_ts"
        private const val KEY_INSTANCE_ID = "instance_id"
    }
}