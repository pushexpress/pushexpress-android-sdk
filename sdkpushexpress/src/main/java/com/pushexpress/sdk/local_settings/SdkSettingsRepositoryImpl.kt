package com.pushexpress.sdk.local_settings

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import java.util.*

class SdkSettingsRepositoryImpl(private val context: Context) : SdkSettingsRepository {

    private val Context.dataStore: DataStore<Preferences> by
                                preferencesDataStore(name = "sdkpushexpress")
    private val instanceToken = stringPreferencesKey(INSTANCE_TOKEN)
    private val installTs = longPreferencesKey(INSTALL_TS)
    private val appId = stringPreferencesKey(APP_ID)
    private val extId = stringPreferencesKey(EXT_ID)
    @Volatile
    private var firebaseToken: String? = null
    private val onscreenCnt = intPreferencesKey(ONSCREEN_CNT)
    private val onscreenSec = longPreferencesKey(ONSCREEN_SEC)
    private val resumedTs = longPreferencesKey(RESUMED_TS)
    private val stoppedTs = longPreferencesKey(STOPPED_TS)

    init {
        runBlocking {
            if (isSdkWasNotInitialized()) {
                context.dataStore.edit { settings ->
                    settings[instanceToken] = UUID.randomUUID().toString()
                    settings[installTs] = System.currentTimeMillis() / 1000
                    settings[appId] = ""
                    settings[extId] = ""
                    settings[onscreenCnt] = 0
                    settings[onscreenSec] = 0
                    settings[resumedTs] = 0
                    settings[stoppedTs] = 0
                }
            }
        }
    }

    override fun saveFirebaseToken(firebaseToken: String) {
        this.firebaseToken = firebaseToken
    }

    override suspend fun savePushExpressExternalId(externalId: String) {
        context.dataStore.edit { settings ->
            settings[this.extId] = externalId
        }
    }

    override suspend fun savePushExpressAppId(pushExpressAppId: String) {
        context.dataStore.edit { settings ->
            settings[this.appId] = pushExpressAppId
        }
    }

    override suspend fun updateAppResumed() {
        context.dataStore.edit { settings ->
            settings[onscreenCnt] = (settings[onscreenCnt] ?: 0) + 1
            settings[resumedTs] = System.currentTimeMillis() / 1000

            Log.d(TAG, "appResumed: ${settings[resumedTs]} " +
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

            Log.d(TAG, "appStopped: ${settings[stoppedTs]} ${settings[onscreenCnt]} ${settings[onscreenSec]}")
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

    private suspend fun isSdkWasNotInitialized() = context.dataStore.data.first()[instanceToken] == null

    companion object {
        private const val INSTANCE_TOKEN = "ic_token"
        private const val INSTALL_TS = "install_ts"
        private const val APP_ID = "app_id"
        private const val EXT_ID = "ext_id"
        private const val ONSCREEN_CNT = "onscreen_cnt"
        private const val ONSCREEN_SEC = "onscreen_sec"
        private const val RESUMED_TS = "resumed_ts"
        private const val STOPPED_TS = "stopped_ts"
        private const val TAG = "SdkPushExpress"
    }
}