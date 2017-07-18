package org.opensilk.video.telly

import android.arch.lifecycle.LifecycleRegistry
import android.arch.lifecycle.LifecycleRegistryOwner
import android.os.Bundle
import android.support.v4.app.FragmentActivity
import org.opensilk.common.dagger.Injector
import org.opensilk.video.UpnpServiceConnectionManager
import java.util.concurrent.atomic.AtomicReference

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
 * Created by drew on 6/1/17.
 */
abstract class BaseVideoActivity: FragmentActivity(), LifecycleRegistryOwner {
    internal val componentReference: AtomicReference<Injector<*>> by lazy {
        AtomicReference<Injector<*>>(lastNonConfigurationInstance as? Injector<*>)
    }
    private val lifecycleRegistry by lazy {
        LifecycleRegistry(this)
    }
    private val upnpServiceHolder: UpnpServiceConnectionManager by lazy {
        UpnpServiceConnectionManager(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(upnpServiceHolder)
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun getLifecycle(): LifecycleRegistry {
        return lifecycleRegistry
    }

    override fun onRetainCustomNonConfigurationInstance(): Any {
        return componentReference.get()
    }

}


