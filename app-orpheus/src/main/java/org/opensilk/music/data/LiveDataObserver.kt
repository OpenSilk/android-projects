package org.opensilk.music.data

import android.arch.lifecycle.Observer

/**
 * Created by drew on 7/29/17.
 */
fun <T> LiveDataObserver(observer: (T) -> Unit): Observer<T> {
    return Observer {
        if (it != null) observer(it)
    }
}