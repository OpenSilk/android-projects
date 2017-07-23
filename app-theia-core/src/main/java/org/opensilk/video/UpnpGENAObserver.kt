package org.opensilk.video

import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleObserver
import android.arch.lifecycle.OnLifecycleEvent
import android.os.AsyncTask
import org.fourthline.cling.controlpoint.SubscriptionCallback
import org.fourthline.cling.model.gena.CancelReason
import org.fourthline.cling.model.gena.GENASubscription
import org.fourthline.cling.model.message.UpnpResponse
import org.fourthline.cling.model.meta.Service
import org.opensilk.upnp.cds.browser.CDSUpnpService
import org.opensilk.upnp.cds.browser.CDSserviceType
import timber.log.Timber
import java.lang.Exception
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Created by drew on 7/23/17.
 */
@Singleton
class UpnpGENAObserver
@Inject constructor(
        private val mUpnpService: CDSUpnpService
) : LifecycleObserver {

    private val mSubscriptions = HashSet<SubscriptionCallback>()

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun onDestroy() {
        for (s in mSubscriptions) {
            s.end()
        }
        mSubscriptions.clear()
    }

    fun subscribeEvents(service: Service<*,*>) {
        if (service.serviceType != CDSserviceType) {
            return
        }
        val callback = object : SubscriptionCallback(service) {
            override fun established(subscription: GENASubscription<out Service<*, *>>) {
            }

            override fun eventReceived(subscription: GENASubscription<out Service<*, *>>) {
                val device = subscription.service.device.details.friendlyName
                for ((key, value) in subscription.currentValues) {
                    Timber.d("$device: ${key}:${value}")
                }
            }

            override fun ended(subscription: GENASubscription<out Service<*, *>>,
                               reason: CancelReason?, responseStatus: UpnpResponse?) {
            }

            override fun eventsMissed(subscription: GENASubscription<out Service<*, *>>,
                                      numberOfMissedEvents: Int) {
            }

            override fun failed(subscription: GENASubscription<out Service<*, *>>,
                                responseStatus: UpnpResponse?, exception: Exception?, defaultMsg: String?) {
            }
        }
        mSubscriptions.add(callback)
        mUpnpService.controlPoint.execute(callback)
    }
}