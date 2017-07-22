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
                            .filter {
                                it.findService(CDSserviceType) != null
                            }.map { toMeta(it) }
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

    private fun toMeta(device: Device<*, *, *>): MediaMeta {
        val meta = MediaMeta()
        meta.mediaId = MediaRef(UPNP_DEVICE, UpnpDeviceId(device.identity.udn.identifierString)).toJson()
        meta.mimeType = MIME_TYPE_CONTENT_DIRECTORY
        meta.title = device.details.friendlyName ?: device.displayString
        meta.subtitle = if (device.displayString == meta.title) "" else device.displayString
        if (device.hasIcons()) {
            var largest = device.icons[0]
            for (ic in device.icons) {
                if (largest.height < ic.height) {
                    largest = ic
                }
            }
            var uri = largest.uri.toString()
            //TODO fragile... only tested on MiniDLNA
            if (uri.startsWith("/")) {
                val ident = device.identity
                if (ident is RemoteDeviceIdentity) {
                    val ru = ident.descriptorURL
                    uri = "http://" + ru.host + ":" + ru.port + uri
                }
            }
            meta.artworkUri = Uri.parse(uri)
        }
        return meta
    }

    private val mListener = object : DefaultRegistryListener() {
        override fun deviceAdded(registry: Registry, device: Device<*, out Device<*, *, *>, out Service<*, *>>) {
            if (device.findService(CDSserviceType) != null) {
                mDatabaseClient.addUpnpDevice(toMeta(device))
            }
        }

        override fun deviceRemoved(registry: Registry, device: Device<*, out Device<*, *, *>, out Service<*, *>>) {
            if (device.findService(CDSserviceType) != null) {
                mDatabaseClient.hideUpnpDevice(device.identity.udn.identifierString)
            }
        }
    }
}