package com.pushexpress.sdk.local_settings

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.pushexpress.sdk.BuildConfig
import com.pushexpress.sdk.main.SDK_TAG
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import java.util.UUID

class SdkSettingsRepositoryImpl(private val context: Context) : SdkSettingsRepository {

    private val Context.dataStore: DataStore<Preferences> by
                                preferencesDataStore(name = STORAGE_NAME)
    private val instanceToken = stringPreferencesKey(INSTANCE_TOKEN)
    private val instanceId = stringPreferencesKey(INSTANCE_ID)
    private val installTs = longPreferencesKey(INSTALL_TS)
    private val appId = stringPreferencesKey(APP_ID)
    private val extId = stringPreferencesKey(EXT_ID)
    @Volatile
    private var firebaseToken: String = ""
    private var transportType: String = ""
    // do i need to put this into the local storage?
    private val tags: MutableMap<String, String> = mutableMapOf()
    private val onscreenCnt = intPreferencesKey(ONSCREEN_CNT)
    private val onscreenSec = longPreferencesKey(ONSCREEN_SEC)
    private val resumedTs = longPreferencesKey(RESUMED_TS)
    private val stoppedTs = longPreferencesKey(STOPPED_TS)

    init {
        runBlocking {
            context.dataStore.edit { settings ->
                if (settings[instanceToken] == null) {
                    zeroSettings(settings)
                }
            }
        }
    }

    override suspend fun saveDeviceInstanceId(instanceId: String) {
        context.dataStore.edit { settings ->
            settings[this.instanceId] = instanceId
        }
    }

    override suspend fun setTag(tagKey: String, tagValue: String) {
        this.tags[tagKey] = tagValue
    }

    override suspend fun saveFirebaseToken(firebaseToken: String) {
        this.firebaseToken = firebaseToken
    }

    override suspend fun savePushExpressExternalId(externalId: String) {
        context.dataStore.edit { settings ->
            settings[this.extId] = externalId
        }
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
                it[instanceId].orEmpty(),
                it[installTs] ?: 0,
                it[appId].orEmpty(),
                it[extId].orEmpty(),
                firebaseToken,
                it[onscreenCnt] ?: 0,
                it[onscreenSec] ?: 0,
                it[resumedTs] ?: 0,
                it[stoppedTs] ?: 0,
                transportType,
                tags
            )
        }.first()
    }

    private fun zeroSettings(settings: MutablePreferences) {
        settings.apply {
            this[instanceToken] = ""
            this[instanceId] = ""
            this[installTs] = 0
            this[appId] = ""
            this[extId] = ""
            this[onscreenCnt] = 0
            this[onscreenSec] = 0
            this[resumedTs] = 0
            this[stoppedTs] = 0
            transportType = ""
            firebaseToken = ""
        }
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
        private const val INSTANCE_ID = "ic_id"
        private const val INSTALL_TS = "install_ts"
        private const val APP_ID = "app_id"
        private const val EXT_ID = "ext_id"
        private const val ONSCREEN_CNT = "onscreen_cnt"
        private const val ONSCREEN_SEC = "onscreen_sec"
        private const val RESUMED_TS = "resumed_ts"
        private const val STOPPED_TS = "stopped_ts"
    }
}