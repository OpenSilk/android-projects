package org.opensilk.video.telly

import android.arch.lifecycle.*
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentActivity
import org.opensilk.common.dagger.Injector
import java.util.concurrent.atomic.AtomicReference
import kotlin.reflect.KClass

/**
 * extra name
 */
const val EXTRA_MEDIAID = "org.opensilk.extra.mediaid"

/**
 *
 */
@Suppress("UNCHECKED_CAST")
fun <T> BaseVideoActivity.daggerComponent(bob: Injector.Factory<T>, foo: T): Injector<T> {
    /*
    val ref = componentReference
    if (ref.get() == null) {
        ref.set(bob.create(foo))
    }
    return ref.get() as Injector<T>
     */
    //Don't keep around for now
    return bob.create(foo)
}

/**
 *
 */
fun <T: ViewModel> Fragment.fetchViewModel(clazz: KClass<T>): T {
    val vm = ViewModelProviders.of(this, (activity.application as ViewModelProvider.Factory)).get(clazz.java)
    if (this is LifecycleRegistryOwner && vm is LifecycleObserver) {
        this.lifecycle.addObserver(vm)
    }
    return vm
}

fun <T: ViewModel> BaseVideoActivity.fetchViewModel(clazz: KClass<T>): T {
    val vm = ViewModelProviders.of(this, (application as ViewModelProvider.Factory)).get(clazz.java)
    if (this is LifecycleRegistryOwner && vm is LifecycleObserver) {
        this.lifecycle.addObserver(vm)
    }
    return vm
}

/**
 * Created by drew on 6/1/17.
 */
abstract class BaseVideoActivity: FragmentActivity(), LifecycleRegistryOwner {
    internal val componentReference: AtomicReference<Injector<*>> by lazy {
        AtomicReference<Injector<*>>(lastNonConfigurationInstance as? Injector<*>)
    }
    private val lifecycleRegistry by lazy {
        LifecycleRegistry(this)
    }

    override fun getLifecycle(): LifecycleRegistry {
        return lifecycleRegistry
    }

    override fun onRetainCustomNonConfigurationInstance(): Any {
        return componentReference.get()
    }

}


