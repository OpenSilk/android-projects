package org.opensilk.video

import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleObserver
import android.arch.lifecycle.OnLifecycleEvent
import org.fourthline.cling.model.message.header.UDAServiceTypeHeader
import org.fourthline.cling.model.meta.Device
import org.fourthline.cling.model.meta.Service
import org.fourthline.cling.registry.DefaultRegistryListener
import org.fourthline.cling.registry.Registry
import org.opensilk.media.UpnpDeviceId
import org.opensilk.media.UpnpFolderId
import org.opensilk.media.newMediaRef
import org.opensilk.upnp.cds.browser.CDSUpnpService
import org.opensilk.upnp.cds.browser.CDSserviceType
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UpnpDevicesObserver
@Inject constructor(
        private val mUpnpService: CDSUpnpService,
        private val mDatabaseClient: DatabaseClient,
        private val mUpnpGENAObserver: UpnpGENAObserver,
        private val mUpnpBrowseScanner: UpnpBrowseScanner
) : DefaultRegistryListener(), LifecycleObserver {

    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    fun preStartCleanup() {
        mDatabaseClient.hideAllUpnpDevices()
        mUpnpService.registry.addListener(this)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun postSearch() {
        mUpnpService.controlPoint.search(UDAServiceTypeHeader(CDSserviceType))
    }

    override fun deviceAdded(registry: Registry, device: Device<*, out Device<*, *, *>, out Service<*, *>>) {
        device.findService(CDSserviceType)?.let {
            val metaDevice = it.device.toMediaMeta()
            val deviceId = newMediaRef(metaDevice.mediaId).mediaId as UpnpDeviceId
            mDatabaseClient.addUpnpDevice(metaDevice)
            mDatabaseClient.postChange(UpnpDeviceChange())
            mUpnpGENAObserver.subscribeEvents(it)
            mUpnpBrowseScanner.enqueue(UpnpFolderId(deviceId.deviceId, "0"))
        }
    }

    override fun deviceRemoved(registry: Registry, device: Device<*, out Device<*, *, *>, out Service<*, *>>) {
        device.findService(CDSserviceType)?.let {
            mDatabaseClient.hideUpnpDevice(it.device.identity.udn.identifierString)
            mDatabaseClient.postChange(UpnpDeviceChange())
            mUpnpGENAObserver.unsubscribeEvents(it)
        }
    }
}