package org.opensilk.video

import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleObserver
import android.arch.lifecycle.OnLifecycleEvent
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.os.SystemClock
import io.reactivex.Single
import io.reactivex.functions.Consumer
import org.fourthline.cling.controlpoint.SubscriptionCallback
import org.fourthline.cling.model.gena.CancelReason
import org.fourthline.cling.model.gena.GENASubscription
import org.fourthline.cling.model.message.UpnpResponse
import org.fourthline.cling.model.message.header.UDAServiceTypeHeader
import org.fourthline.cling.model.meta.Device
import org.fourthline.cling.model.meta.Service
import org.fourthline.cling.model.types.ServiceId
import org.fourthline.cling.registry.DefaultRegistryListener
import org.fourthline.cling.registry.Registry
import org.opensilk.reactivex2.subscribeIgnoreError
import org.opensilk.media.UpnpDeviceId
import org.opensilk.upnp.cds.browser.CDSGetSystemUpdateIDAction
import org.opensilk.upnp.cds.browser.CDSUpnpService
import org.opensilk.upnp.cds.browser.CDSserviceType
import timber.log.Timber
import java.lang.ref.WeakReference
import javax.inject.Inject
import javax.inject.Singleton

private val GRACE_PERIOD = 600_000L //10min

@Singleton
class UpnpDevicesObserver
@Inject constructor(
        private val mUpnpService: CDSUpnpService,
        private val mDatabaseClient: DatabaseClient
) : DefaultRegistryListener(), LifecycleObserver {

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onStart() {
        //cancel pause
        mHandler.removeCallbacksAndMessages(null)

        //un pause
        mUpnpService.router.enable()
        mUpnpService.registry.resume()

        //reset devices and post new search
        mDatabaseClient.hideAllUpnpDevices()
        mDatabaseClient.postChange(UpnpDeviceChange())
        mUpnpService.registry.removeAllRemoteDevices()
        mUpnpService.registry.removeAllLocalDevices()
        mUpnpService.registry.addListener(this)
        mUpnpService.controlPoint.search(UDAServiceTypeHeader(CDSserviceType))
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onStop() {
        //stop listening for devices
        mUpnpService.registry.removeListener(this)

        //stop listening for events
        unsubscribeAllEvents()

        //post pause task
        mHandler.removeCallbacksAndMessages(null)
        mHandler.sendEmptyMessageDelayed(1, GRACE_PERIOD / 2)
    }

    /**
     * Called by registry when device added
     */
    override fun deviceAdded(registry: Registry, device: Device<*, out Device<*, *, *>, out Service<*, *>>) {
        device.findService(CDSserviceType)?.let { service ->
            val metaDevice = service.device.toMediaMeta()
            Timber.d("Found new CDS ${metaDevice.meta.title}")
            mDatabaseClient.addUpnpDevice(metaDevice)
            mDatabaseClient.postChange(UpnpDeviceChange())
            subscribeEvents(service)
        }
    }

    /**
     * Called by registry when device removed
     */
    override fun deviceRemoved(registry: Registry, device: Device<*, out Device<*, *, *>, out Service<*, *>>) {
        device.findService(CDSserviceType)?.let {
            mDatabaseClient.hideUpnpDevice(it.device.identity.udn.identifierString)
            mDatabaseClient.postChange(UpnpDeviceChange())
            unsubscribeEvents(it)
        }
    }

    /**
     * Fetches the SystemUpdateId from the CDS
     */
    private fun updateId(service: Service<*,*>): Single<Long> {
        return Single.create { s ->
            val action = CDSGetSystemUpdateIDAction(mUpnpService.controlPoint, service)
            action.run()
            if (s.isDisposed) {
                return@create
            }
            if (action.error.get() != null) {
                s.onError(action.error.get())
                return@create
            }
            if (action.result.get() == null) {
                s.onError(NullPointerException())
                return@create
            }
            s.onSuccess(action.result.get().id.value)
        }
    }

    private val mSubscriptions = HashMap<ServiceId, SubscriptionCallback>()

    fun subscribeEvents(service: Service<*,*>) {
        if (service.serviceType != CDSserviceType) {
            return
        }
        val callback = object : SubscriptionCallback(service) {
            override fun established(subscription: GENASubscription<out Service<*, *>>) {
            }

            override fun eventReceived(subscription: GENASubscription<out Service<*, *>>) {
                val device = subscription.service.device
                val values = subscription.currentValues
                for ((key, value) in values) {
                    Timber.d("${device.details.friendlyName}: $key: $value")
                }
                if (values.containsKey("SystemUpdateID")) {
                    handleSystemUpdateId(
                            values["SystemUpdateID"]?.value?.toString()?.toLong(),
                            UpnpDeviceId(device.identity.udn.identifierString))
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
        mUpnpService.controlPoint.execute(callback)
        synchronized(mSubscriptions) {
            mSubscriptions.put(service.serviceId, callback)
        }
    }

    fun unsubscribeEvents(service: Service<*,*>) {
        synchronized(mSubscriptions) {
            mSubscriptions.remove(service.serviceId)?.end()
        }
    }

    fun unsubscribeAllEvents() {
        synchronized(mSubscriptions) {
            mSubscriptions.values.forEach { s ->
                s.end()
            }
            mSubscriptions.clear()
        }
    }

    /**
     * compares new updateId with old value, and updates if changed
     */
    private fun handleSystemUpdateId(updateId: Long?, deviceId: UpnpDeviceId) {
        if (updateId == null) return
        mDatabaseClient.getUpnpDeviceSystemUpdateId(deviceId)
                .subscribeIgnoreError(Consumer { oldId ->
                    if (oldId != updateId) {
                        mDatabaseClient.setUpnpDeviceSystemUpdateId(deviceId, updateId)
                        //TODO attach the deviceId to the change
                        mDatabaseClient.postChange(UpnpUpdateIdChange(updateId))
                    }
                })
    }

    private val mHandler = SuspendHandler(WeakReference(mUpnpService))

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