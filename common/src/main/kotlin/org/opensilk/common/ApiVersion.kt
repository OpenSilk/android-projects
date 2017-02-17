package org.opensilk.common

import android.annotation.TargetApi
import android.os.Build

/**
 * Created by drew on 6/27/16.
 */
public object ApiVersion {
    val isAtLeastApi21: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP

    val isAtLeastLollipop: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP

    val isAtLeastApi22: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1

    val isAtLeastLollipopMR1: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1

    @JvmStatic val isAtLeastApi23: Boolean
        @TargetApi(Build.VERSION_CODES.M) get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M

    val isAtLeastMarshmallow: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
}