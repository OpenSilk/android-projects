package org.opensilk.common

import android.os.Bundle
import android.os.Parcelable

fun bundle(key: String, data: Int): Bundle {
    return Bundle()._putInt(key, data)
}

fun Bundle._putInt(key: String, data: Int): Bundle {
    this.putInt(key, data)
    return this
}

fun bundle(key: String, data: String): Bundle {
    return Bundle()._putString(key, data)
}

fun Bundle._putString(key: String, data: String): Bundle {
    this.putString(key, data)
    return this;
}

fun <T: Parcelable> bundle(key: String, data: T): Bundle {
    return Bundle()._putParcelable(key, data)
}

fun <T: Parcelable> Bundle._putParcelable(key: String, data: T): Bundle {
    this.putParcelable(key, data)
    return this
}