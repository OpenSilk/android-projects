package org.opensilk.music.viewmodel

import android.arch.lifecycle.*
import android.support.v4.app.Fragment
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

/**
 * Created by drew on 9/5/17.
 */
class FragmentViewModelDelegate<out T: ViewModel>(private val clazz: KClass<T>): ReadOnlyProperty<Fragment, T> {
    private var cached: T? = null

    override fun getValue(thisRef: Fragment, property: KProperty<*>): T {
        return cached ?: thisRef.fetchViewModel(clazz).also {
            cached = it
        }
    }
}

fun <T: ViewModel> Fragment.fetchViewModel(clazz: KClass<T>): T {
    val factory = activity.application as ViewModelProvider.Factory
    val vm = ViewModelProviders.of(this, factory).get(clazz.java)
    if (this is LifecycleRegistryOwner && vm is LifecycleObserver) {
        this.lifecycle.addObserver(vm)
    }
    return vm
}

/**
 * Magic method that provides the [ViewModel] for a fragment
 */
inline fun <reified T: ViewModel> fragmentViewModel(): FragmentViewModelDelegate<T> =
        FragmentViewModelDelegate(T::class)
