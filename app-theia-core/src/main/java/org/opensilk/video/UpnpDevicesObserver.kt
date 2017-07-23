package org.opensilk.video

import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleObserver
import android.arch.lifecycle.OnLifecycleEvent
import android.net.Uri
import org.fourthline.cling.model.message.header.STAllHeader
import org.fourthline.cling.model.message.header.UDAServiceTypeHeader
import org.fourthline.cling.model.meta.Device
import org.fourthline.cling.model.meta.RemoteDeviceIdentity
import org.fourthline.cling.model.meta.Service
import org.fourthline.cling.model.types.UDAServiceType
import org.fourthline.cling.registry.DefaultRegistryListener
import org.fourthline.cling.registry.Registry
import org.opensilk.media.*
import org.opensilk.upnp.cds.browser.CDSUpnpService
import org.opensilk.upnp.cds.browser.CDSserviceType
import rx.Observable
import timber.log.Timber
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.collections.ArrayList

@Singleton
class UpnpDevicesObserver
@Inject constructor(
        private val mUpnpService: CDSUpnpService,
        private val mDatabaseClient: DatabaseClient,
        private val mUpnpGENAObserver: UpnpGENAObserver
) : LifecycleObserver {

    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    fun compareContrast() {
        mDatabaseClient.hideAllUpnpDevices()
        mUpnpService.registry.addListener(mListener)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun stopListening() {
        mUpnpService.registry.removeListener(mListener)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun postSearch() {
        mUpnpService.controlPoint.search(UDAServiceTypeHeader(CDSserviceType))
    }

    private val mListener = object : DefaultRegistryListener() {
        override fun deviceAdded(registry: Registry, device: Device<*, out Device<*, *, *>, out Service<*, *>>) {
            device.findService(CDSserviceType)?.let {
                mDatabaseClient.addUpnpDevice(it.device.toMediaMeta())
                mDatabaseClient.postChange(UpnpDeviceChange())
                mUpnpGENAObserver.subscribeEvents(it)
            }
        }

        override fun deviceRemoved(registry: Registry, device: Device<*, out Device<*, *, *>, out Service<*, *>>) {
            device.findService(CDSserviceType)?.let {
                mDatabaseClient.hideUpnpDevice(it.device.identity.udn.identifierString)
                mDatabaseClient.postChange(UpnpDeviceChange())
            }
        }
    }
}