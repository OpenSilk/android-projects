package org.opensilk.video.telly

import android.app.Activity
import android.app.Application
import android.arch.lifecycle.*
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.provider.ContactsContract
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentActivity
import dagger.Module
import org.eclipse.jetty.util.component.LifeCycle
import org.opensilk.common.dagger.ForActivity
import org.opensilk.common.dagger.Injector
import org.opensilk.common.dagger.injectMe
import org.opensilk.video.UpnpHolderService
import org.opensilk.video.UpnpServiceConnectionManager
import timber.log.Timber
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject

/**
 *
 */
@Suppress("UNCHECKED_CAST")
fun <T> BaseVideoActivity.daggerComponent(bob: Injector.Factory<T>, foo: T): Injector<T> {
    val ref = componentReference
    if (ref.get() == null) {
        ref.set(bob.create(foo))
    }
    return ref.get() as Injector<T>
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
