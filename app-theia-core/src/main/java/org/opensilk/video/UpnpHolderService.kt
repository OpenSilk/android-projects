package org.opensilk.video

import android.app.Activity
import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleObserver
import android.arch.lifecycle.LifecycleService
import android.arch.lifecycle.OnLifecycleEvent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Binder
import android.os.IBinder
import dagger.Module
import dagger.Subcomponent
import org.opensilk.common.dagger.Injector
import org.opensilk.common.dagger.ServiceScope
import org.opensilk.common.dagger.injectMe
import org.opensilk.upnp.cds.browser.CDSUpnpService
import timber.log.Timber
import javax.inject.Inject

/**
 *
 */
@ServiceScope
@Subcomponent
interface UpnpHolderServiceComponent: Injector<UpnpHolderService> {
    @Subcomponent.Builder
    abstract class Builder : Injector.Builder<UpnpHolderService>()
}

/**
 *
 */
@Module(subcomponents = arrayOf(UpnpHolderServiceComponent::class))
abstract class UpnpHolderServiceModule

/**
 *
 */
class UpnpServiceConnectionManager
constructor(
        val activity: Activity
): ServiceConnection, LifecycleObserver {
    private var mUpnpServiceHolder : UpnpHolderService.HolderBinder? = null

    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    fun onCreate() {
        if (!activity.bindService(Intent(activity, UpnpHolderService::class.java), this, Context.BIND_AUTO_CREATE)) {
            Timber.e("Failed to bind to the UpnpHolderService")
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun onDestroy() {
        activity.unbindService(this)
        mUpnpServiceHolder = null
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        mUpnpServiceHolder = null
    }

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        mUpnpServiceHolder = service as? UpnpHolderService.HolderBinder
    }
}

/**
 * Service that holds a reference to the upnpservice so it can be shutdown
 */
class UpnpHolderService: LifecycleService() {
    private val mBinder = HolderBinder()
    @Inject lateinit var mUpnpService: CDSUpnpService
    @Inject lateinit var mUpnpDevicesObserver: UpnpDevicesObserver

    override fun onCreate() {
        super.onCreate()
        injectMe()
        lifecycle.addObserver(mUpnpDevicesObserver)
    }

    override fun onDestroy() {
        super.onDestroy()
        mUpnpService.shutdown()
    }

    override fun onBind(intent: Intent?): IBinder? {
        super.onBind(intent)
        return mBinder
    }

    class HolderBinder: Binder()
}