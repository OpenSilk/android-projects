package org.opensilk.video

import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleObserver
import android.arch.lifecycle.OnLifecycleEvent
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.os.SystemClock
import org.opensilk.upnp.cds.browser.CDSUpnpService
import java.lang.ref.WeakReference
import javax.inject.Inject
import javax.inject.Singleton

private val GRACE_PERIOD = 600_000L //10min
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
    private val mHandler = SuspendHandler(WeakReference(mUpnpService))

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onStart() {
        mUpnpService.router.enable()
        mUpnpService.registry.resume()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onStop() {
        mHandler.removeCallbacksAndMessages(null)
        mHandler.sendEmptyMessageDelayed(1, GRACE_PERIOD / 2)
    }

    private class SuspendHandler(val ref: WeakReference<CDSUpnpService>) : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            val service = ref.get() ?: return
            if ((service.lastUsed + GRACE_PERIOD) < SystemClock.elapsedRealtime()) {
                service.registry.pause()
                service.router.disable()
            } else {
                this.sendEmptyMessageDelayed(1, GRACE_PERIOD / 2)
            }
        }
    }
}