package com.pushexpress.sdk.utils

import android.util.Log
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
            Log.d("retryHttpIO", "io-error: $e, delay for ${currentDelay}ms")
        } catch (e: retrofit2.HttpException) {
            Log.d("retryHttpIO", "http-error: $e, delay for ${currentDelay}ms")
        }
        delay(currentDelay)
        currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
    } }
    Log.d("retryHttpIO", "last run whatever happens ...")
    return block()
}