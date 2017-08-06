package org.opensilk.media

import android.os.Bundle
import android.os.IBinder
import android.os.Parcelable

/**
 * Created by drew on 6/28/16.
 */

fun bundle(): Bundle = Bundle()

fun bundle(key: String, data: Int): Bundle {
    return bundle()._putInt(key, data)
}

fun Bundle._putInt(key: String, data: Int): Bundle {
    this.putInt(key, data)
    return this
}

fun Bundle._putLong(key: String, data: Long): Bundle {
    this.putLong(key, data)
    return this
}

fun bundle(key: String, data: String): Bundle {
    return bundle()._putString(key, data)
}

fun Bundle._putString(key: String, data: String): Bundle {
    this.putString(key, data)
    return this;
}

fun <T: Parcelable> bundle(key: String, data: T): Bundle {
    return bundle()._putParcelable(key, data)
}

fun <T: Parcelable> Bundle._putParcelable(key: String, data: T): Bundle {
    this.putParcelable(key, data)
    return this
}

fun <T: IBinder> Bundle._putBinder(key: String, data: T): Bundle {
    this.putBinder(key, data)
    return this
}