package org.opensilk.dagger2

import android.content.Context

/**
 * Allows any class with access to the context to be injectable
 *
 * Created by drew on 8/26/17.
 */
fun Any.injectMe(context: Context) {
    val app = context.applicationContext
    if (app is InjectionManager) {
        app.injectFoo(this)
    } else TODO("Application is not an InjectionManager")
}