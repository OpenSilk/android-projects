package org.opensilk.common

import android.content.Context
import android.net.ConnectivityManager

/**
 * Created by drew on 6/28/16.
 */

fun Context.isConnectedToInternet(): Boolean {
    val ni = connectivityManager().activeNetworkInfo
    return ni != null && ni.isConnectedOrConnecting
}

fun Context.isConnectedToWifiOrEtherne(): Boolean {
    val ni = connectivityManager().activeNetworkInfo
    return ni != null && (ni.type == ConnectivityManager.TYPE_WIFI || ni.type == ConnectivityManager.TYPE_ETHERNET)
}

fun Context.connectivityManager(): ConnectivityManager {
    return getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
}