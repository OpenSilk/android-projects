package org.opensilk.video

import android.arch.lifecycle.*
import android.support.v4.app.Fragment
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

/**
 * Created by drew on 9/5/17.
 */
class ViewModelDelegate<out T: ViewModel>(private val clazz: KClass<T>): ReadOnlyProperty<Fragment, T> {
    private var cached: T? = null

    override fun getValue(thisRef: Fragment, property: KProperty<*>): T {
        return cached ?: thisRef.getViewModel(clazz).also {
            cached = it
        }
    }
}

fun <T: ViewModel> Fragment.getViewModel(clazz: KClass<T>): T {
    val vm = ViewModelProviders.of(this, (activity.application as ViewModelProvider.Factory)).get(clazz.java)
    if (vm is LifecycleObserver) {
        this.lifecycle.addObserver(vm)
    }
    return vm
}

inline fun <reified T: ViewModel> fragmentViewModel(): ViewModelDelegate<T> = ViewModelDelegate(T::class)