package org.opensilk.video

import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleObserver
import android.arch.lifecycle.OnLifecycleEvent
import org.fourthline.cling.model.message.header.UDAServiceTypeHeader
import org.fourthline.cling.model.meta.Device
import org.fourthline.cling.model.meta.Service
import org.fourthline.cling.registry.DefaultRegistryListener
import org.fourthline.cling.registry.Registry
import org.opensilk.media.MediaMeta
import org.opensilk.media.UpnpDeviceId
import org.opensilk.media.UpnpFolderId
import org.opensilk.media.newMediaRef
import org.opensilk.upnp.cds.browser.CDSGetSystemUpdateIDAction
import org.opensilk.upnp.cds.browser.CDSUpnpService
import org.opensilk.upnp.cds.browser.CDSserviceType
import rx.Single
import timber.log.Timber
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

    private class UpnpDeviceUpdateIdScanning(val device: MediaMeta, val updateId: Long, val scanning: Long)

    override fun deviceAdded(registry: Registry, device: Device<*, out Device<*, *, *>, out Service<*, *>>) {
        device.findService(CDSserviceType)?.let { service ->
            val metaDevice = service.device.toMediaMeta()
            val deviceId = newMediaRef(metaDevice.mediaId).mediaId as UpnpDeviceId
            Single.zip(
                    mDatabaseClient.getUpnpDevice(deviceId).onErrorReturn { metaDevice },
                    updateId(service).onErrorReturn { 0 },
                    mDatabaseClient.getUpnpDeviceScanning(deviceId).onErrorReturn { -1 },
                    { dev, id, s -> UpnpDeviceUpdateIdScanning(dev, id, s) }
            ).subscribe({ dwu ->
                Timber.i("${metaDevice.title} SystemUpdateID: old=${dwu.device.updateId} new=${dwu.updateId}")
                val changed = dwu.updateId != dwu.device.updateId
                val scanning = dwu.scanning != 0L
                metaDevice.updateId = dwu.updateId
                mDatabaseClient.addUpnpDevice(metaDevice)
                mDatabaseClient.postChange(UpnpDeviceChange())
                mUpnpGENAObserver.subscribeEvents(service)
                if (changed || scanning) {
                    mUpnpBrowseScanner.enqueue(UpnpFolderId(deviceId.deviceId, "0"))
                }
            })
        }
    }

    override fun deviceRemoved(registry: Registry, device: Device<*, out Device<*, *, *>, out Service<*, *>>) {
        device.findService(CDSserviceType)?.let {
            mDatabaseClient.hideUpnpDevice(it.device.identity.udn.identifierString)
            mDatabaseClient.postChange(UpnpDeviceChange())
            mUpnpGENAObserver.unsubscribeEvents(it)
        }
    }

    /**
     * Fetches the SystemUpdateId from the CDS
     */
    private fun updateId(service: Service<*,*>): Single<Long> {
        return Single.create { s ->
            val action = CDSGetSystemUpdateIDAction(mUpnpService.controlPoint, service)
            action.run()
            if (s.isUnsubscribed) {
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
}