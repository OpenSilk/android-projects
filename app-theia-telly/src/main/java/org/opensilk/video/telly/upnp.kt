package org.opensilk.video.telly

import android.content.Intent
import android.media.MediaDescription
import android.media.browse.MediaBrowser
import android.net.Uri
import android.os.IBinder
import org.fourthline.cling.model.message.header.UDAServiceTypeHeader
import org.fourthline.cling.model.meta.Device
import org.fourthline.cling.model.meta.RemoteDeviceIdentity
import org.fourthline.cling.model.meta.Service
import org.fourthline.cling.model.types.UDAServiceType
import org.fourthline.cling.registry.DefaultRegistryListener
import org.fourthline.cling.registry.Registry
import org.opensilk.common.dagger.ActivityScope
import org.opensilk.common.loader.RxListLoader
import org.opensilk.common.loader.RxLoader
import org.opensilk.media.MediaMeta
import org.opensilk.media._setMediaMeta
import org.opensilk.upnp.cds.browser.CDSUpnpService
import rx.Observable
import rx.subscriptions.Subscriptions
import javax.inject.Inject

/**
 * Created by drew on 5/29/17.
 */
@ActivityScope
class CDSDevicesLoader
@Inject constructor(
        private val mUpnpService: CDSUpnpService
): RxLoader<MediaBrowser.MediaItem> {
    override val observable: Observable<MediaBrowser.MediaItem> by lazy {
        Observable.create<MediaBrowser.MediaItem> { s ->
            val listener = object : DefaultRegistryListener() {
                override fun deviceAdded(registry: Registry, device: Device<*, out Device<*, *, *>, out Service<*, *>>) {
                    if (device.findService(UDAServiceType("ContentDirectory", 1)) == null) {
                        //unsupported device
                        return
                    }
                    val id = device.identity.udn.identifierString
                    val label = device.details.friendlyName ?: device.displayString
                    val subtitle = if (device.displayString == label) "" else device.displayString
                    val mediaExtras = MediaMeta()
                    val builder = MediaDescription.Builder()
                            .setTitle(label)
                            .setSubtitle(subtitle)
                            ._mediaRef(MediaRef(UPNP_DEVICE, id))
                            ._setMediaMeta(mediaExtras)
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
                        builder.setIconUri(Uri.parse(uri))
                    }
                    s.onNext(MediaBrowser.MediaItem(builder.build(), MediaBrowser.MediaItem.FLAG_BROWSABLE))
                }

                override fun deviceRemoved(registry: Registry, device: Device<*, out Device<*, *, *>, out Service<*, *>>) {
                    if (device.findService(UDAServiceType("ContentDirectory", 1)) == null) {
                        //dont care
                        return
                    }
                    s.onError(DeviceRemovedException())
                }
            }
            s.add(Subscriptions.create { mUpnpService.registry.removeListener(listener) })
            mUpnpService.registry.addListener(listener)
            for (device in mUpnpService.registry.devices) {
                //pass through all the already found ones
                listener.deviceAdded(mUpnpService.registry, device)
            }
            //find new devices
            mUpnpService.controlPoint.search(UDAServiceTypeHeader(UDAServiceType("ContentDirectory", 1)))
        }
    }
}

/**
 * Exception used to notify subscribers a device was removed
 * they will need to clear their cache and resubscribe
 */
class DeviceRemovedException : Exception()

/**
 * Service that holds a reference to the upnpservice so it can be shutdown
 */
class UpnpHolderService: android.app.Service() {
    lateinit var mUpnpService: CDSUpnpService

    override fun onCreate() {
        super.onCreate()
        mUpnpService = rootComponent().upnpService()
    }

    override fun onDestroy() {
        super.onDestroy()
        mUpnpService.shutdown()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}

class CDSBrowseLoader : RxListLoader<MediaBrowser.MediaItem> {
    override val listObservable: Observable<List<MediaBrowser.MediaItem>>
        get() = TODO("not implemented")
}