package com.pushexpress.sdk.utils

import android.util.Log
import com.pushexpress.sdk.BuildConfig
import com.pushexpress.sdk.main.SDK_TAG
import kotlinx.coroutines.delay
import java.io.IOException
import java.util.concurrent.TimeUnit

suspend fun <T> retryHttpIO(
    times: Int = 3,
    initialDelay: Long = TimeUnit.SECONDS.toMillis(2),
    maxDelay: Long = TimeUnit.SECONDS.toMillis(64),
    factor: Double = 2.0,
    block: suspend () -> T
): T {
    var currentDelay = initialDelay
    run repeatBlock@ { repeat(times - 1) {
        try {
            return block()
        } catch (e: IOException) {
            if (BuildConfig.LOG_DEBUG) Log.d(SDK_TAG, "Retry io-error: $e, delay for ${currentDelay}ms")
        } catch (e: retrofit2.HttpException) {
            if (BuildConfig.LOG_DEBUG) Log.d(SDK_TAG, "Retry http-error: $e, delay for ${currentDelay}ms")
        }
        delay(currentDelay)
        currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
    } }
    if (BuildConfig.LOG_DEBUG) Log.d(SDK_TAG, "Last run whatever happens ...")
    return block()
}


inline fun <reified T : Any?> getBy(value: Boolean, primary: T, secondary: T): T =
    if (value) primary else secondary

inline fun <reified T : Any?> getBy(value: () -> Boolean, primary: T, secondary: T): T =
    if (value()) primary else secondary

inline fun <reified T : Any?> getBy(value: () -> Boolean, primary: () -> T, secondary: () -> T): T =
    if (value()) primary() else secondary()

inline fun <reified T : Any?> getBy(value: Boolean, primary: () -> T, secondary: () -> T): T =
    if (value) primary() else secondary()
