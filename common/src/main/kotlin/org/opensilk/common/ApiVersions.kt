package org.opensilk.common

import android.os.Build

val isAtLeastApi16: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN

val isAtLeastJellybean: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN

val isAtLeastApi19: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT

val isAtLeastKitkat: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT

val isAtLeastApi21: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP

val isAtLeastLollipop: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP

val isAtLeastApi22: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1

val isAtLeastLollipopMR1: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1

val isAtLeastApi23: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M

val isAtLeastMarshmallow: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M