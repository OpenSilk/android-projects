package org.opensilk.common

import android.content.Context
import android.net.ConnectivityManager

/**
 * Created by drew on 6/28/16.
 */

fun Context.connectivityManager(): ConnectivityManager {
    return getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
}