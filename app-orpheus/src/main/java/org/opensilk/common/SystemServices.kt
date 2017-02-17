package org.opensilk.common

import android.content.Context
import android.net.ConnectivityManager

fun Context.connectivityManager(): ConnectivityManager {
    return getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
}