package org.opensilk.video

import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleObserver
import android.arch.lifecycle.OnLifecycleEvent
import android.net.Uri
import org.fourthline.cling.model.meta.Device
import org.fourthline.cling.model.meta.RemoteDeviceIdentity
import org.fourthline.cling.model.meta.Service
import org.fourthline.cling.registry.DefaultRegistryListener
import org.fourthline.cling.registry.Registry
import org.opensilk.media.*
import org.opensilk.upnp.cds.browser.CDSUpnpService
import org.opensilk.upnp.cds.browser.CDSserviceType
import java.util.*
import javax.inject.Inject

class UpnpDevicesObserver
@Inject constructor(
        private val mUpnpService: CDSUpnpService,
        private val mDatabaseClient: DatabaseClient
) : LifecycleObserver {

    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    fun startListening() {
        mDatabaseClient.getUpnpDevices()
                .subscribeOn(AppSchedulers.diskIo)
                .toList()
                .doOnTerminate {
                    mUpnpService.registry.addListener(mListener)
                }
                .subscribe { list ->
                    val devices = LinkedList(list)
                    val external = mUpnpService.registry.devices
                            .filter { it.findService(CDSserviceType) != null }.map { it.toMediaMeta() }
                    for (ext in external) {
                        val dev = devices.find { it.mediaId == ext.mediaId }
                        if (dev != null) {
                            devices.remove(dev)
                        }
                        mDatabaseClient.addUpnpDevice(ext)
                    }
                    for (dev in devices) {
                        val mediaId = newMediaRef(dev.mediaId).mediaId
                        if (mediaId is UpnpDeviceId) {
                            mDatabaseClient.hideUpnpDevice(mediaId.deviceId)
                        }
                    }
                    mDatabaseClient.notifyChange(mDatabaseClient.uris.upnpDevices())
                }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun stopListening() {
        mUpnpService.registry.removeListener(mListener)
    }



    private val mListener = object : DefaultRegistryListener() {
        override fun deviceAdded(registry: Registry, device: Device<*, out Device<*, *, *>, out Service<*, *>>) {
            if (device.findService(CDSserviceType) != null) {
                mDatabaseClient.addUpnpDevice(device.toMediaMeta())
            }
        }

        override fun deviceRemoved(registry: Registry, device: Device<*, out Device<*, *, *>, out Service<*, *>>) {
            if (device.findService(CDSserviceType) != null) {
                mDatabaseClient.hideUpnpDevice(device.identity.udn.identifierString)
            }
        }
    }
}