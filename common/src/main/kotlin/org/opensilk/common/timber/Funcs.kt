package org.opensilk.common.timber


internal fun appendThreadName(msg: String): String {
    val threadName = Thread.currentThread().name
    if ("main" == threadName) {
        return msg
    }
    return "$msg [$threadName]"
}