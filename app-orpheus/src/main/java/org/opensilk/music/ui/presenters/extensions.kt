package org.opensilk.music.ui.presenters

import android.databinding.DataBindingUtil
import android.databinding.ViewDataBinding
import android.support.annotation.LayoutRes
import android.support.v7.app.AppCompatActivity

/**
 * Created by drew on 2/25/17.
 */
fun <T : ViewDataBinding> AppCompatActivity.createBinding(@LayoutRes layout: Int): Lazy<T> {
    return lazy {
        DataBindingUtil.setContentView<T>(this, layout)
    }
}