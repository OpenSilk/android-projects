package org.opensilk.video

import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleObserver
import android.arch.lifecycle.OnLifecycleEvent
import org.opensilk.common.dagger.ServiceScope
import org.opensilk.upnp.cds.browser.CDSUpnpService
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Created by drew on 7/23/17.
 *
 * pauses and resumes the upnp registry when the process is backgrounded/foregrounded
 *
 * This observer should listen to the process lifecycle
 */
@Singleton
class UpnpMaintenanceObserver
@Inject constructor(
        private val mUpnpService: CDSUpnpService
): LifecycleObserver {
    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onStart() {
        mUpnpService.router.enable()
        mUpnpService.registry.resume()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onStop() {
        mUpnpService.registry.pause()
        mUpnpService.router.disable()
    }
}