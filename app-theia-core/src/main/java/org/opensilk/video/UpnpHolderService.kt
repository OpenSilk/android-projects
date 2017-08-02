package org.opensilk.video

import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.ProcessLifecycleOwner
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import dagger.Module
import dagger.Subcomponent
import org.opensilk.common.dagger.Injector
import org.opensilk.common.dagger.ServiceScope
import org.opensilk.common.dagger.injectMe
import org.opensilk.upnp.cds.browser.CDSUpnpService
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
 * Service that holds a reference to the upnpservice so it can be shutdown
 */
class UpnpHolderService: android.app.Service() {
    private val lifecycle: Lifecycle by lazy {
        ProcessLifecycleOwner.get().lifecycle
    }
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
        return mBinder
    }

    class HolderBinder: Binder()
}