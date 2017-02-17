package org.opensilk.common

import android.content.Context
import android.net.ConnectivityManager


fun Context.isConnectedToInternet(): Boolean {
    val ni = connectivityManager().activeNetworkInfo
    return ni != null && ni.isConnectedOrConnecting
}

fun Context.isConnectedToWifiOrEthernet(): Boolean {
    val ni = connectivityManager().activeNetworkInfo
    return ni != null && (ni.type == ConnectivityManager.TYPE_WIFI || ni.type == ConnectivityManager.TYPE_ETHERNET)
}
