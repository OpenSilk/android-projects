package org.opensilk.logging

import android.os.StrictMode
import timber.log.Timber

fun installLogging(debug: Boolean) {
    if (debug) {
        Timber.plant(DebugTreeWithThreadName)
        enableStrictMode()
    } else {
        Timber.plant(ReleaseTreeWithThreadName)
    }
}

fun enableStrictMode() {
    val threadPolicyBuilder = StrictMode.ThreadPolicy.Builder().detectAll().penaltyLog().penaltyFlashScreen()
    StrictMode.setThreadPolicy(threadPolicyBuilder.build())

    val vmPolicyBuilder = StrictMode.VmPolicy.Builder().detectAll().penaltyLog()
    StrictMode.setVmPolicy(vmPolicyBuilder.build())
}

internal fun appendThreadName(msg: String): String {
    val threadName = Thread.currentThread().name
    if ("main" == threadName) {
        return msg
    }
    return "$msg [$threadName]"
}
