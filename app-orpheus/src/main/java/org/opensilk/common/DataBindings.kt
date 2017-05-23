
package org.opensilk.common

import android.app.Activity
import android.databinding.DataBindingUtil
import android.databinding.ViewDataBinding
import android.support.annotation.LayoutRes

/**
 * Created by drew on 6/29/16.
 */

fun <T: ViewDataBinding> Activity.bindLayout(@LayoutRes layout: Int): T {
    return DataBindingUtil.setContentView(this, layout)
}

fun <T : ViewDataBinding> Activity.lazyBindLayout(@LayoutRes layout: Int): Lazy<T> {
    return lazy {
        DataBindingUtil.setContentView<T>(this, layout)
    }
}