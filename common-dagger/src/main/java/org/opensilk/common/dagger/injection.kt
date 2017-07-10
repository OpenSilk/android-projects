package org.opensilk.common.dagger

import android.app.Activity
import android.app.Fragment
import android.app.Service
import android.content.ContentProvider

/**
 * Created by drew on 5/30/17.
 */
interface InjectionManager {
    /**
     * foo is the object to inject
     * returns the component associated with foo
     */
    fun injectFoo(foo: Any): Any
}

fun Activity.injectMe() {
    val app = application
    if (app is InjectionManager) {
        app.injectFoo(this)
    } else {
        TODO("App needs to be magic injectorManager")
    }
}

fun Fragment.injectMe() {
    val app = activity.application
    if (app is InjectionManager) {
        app.injectFoo(this)
    } else {
        TODO("App needs to be an InjectionManager")
    }

}

fun Service.injectMe() {
    val app = application
    if (app is InjectionManager) {
        app.injectFoo(this)
    } else {
        TODO("App needs to be an InjectionManager")
    }
}

fun ContentProvider.injectMe() {
    val app = context!!.applicationContext
    if (app is InjectionManager) {
        app.injectFoo(this)
    } else {
        TODO("App needs to be an InjectionManager")
    }
}