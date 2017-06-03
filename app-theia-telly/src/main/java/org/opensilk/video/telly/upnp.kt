package org.opensilk.video.telly

import android.content.Intent
import android.media.MediaDescription
import android.media.browse.MediaBrowser
import android.net.Uri
import android.os.Binder
import android.os.IBinder
import android.provider.DocumentsContract
import android.view.TouchDelegate
import dagger.Binds
import dagger.BindsInstance
import dagger.Module
import dagger.Subcomponent
import org.fourthline.cling.android.AndroidUpnpService
import org.fourthline.cling.model.action.ActionInvocation
import org.fourthline.cling.model.message.UpnpResponse
import org.fourthline.cling.model.message.header.UDAServiceTypeHeader
import org.fourthline.cling.model.meta.*
import org.fourthline.cling.model.types.UDAServiceType
import org.fourthline.cling.model.types.UDN
import org.fourthline.cling.model.types.UnsignedIntegerFourBytes
import org.fourthline.cling.registry.DefaultRegistryListener
import org.fourthline.cling.registry.Registry
import org.fourthline.cling.support.contentdirectory.callback.Browse
import org.fourthline.cling.support.model.BrowseFlag
import org.fourthline.cling.support.model.DIDLContent
import org.fourthline.cling.support.model.container.MusicAlbum
import org.fourthline.cling.support.model.container.MusicArtist
import org.fourthline.cling.support.model.container.StorageFolder
import org.fourthline.cling.support.model.item.MusicTrack
import org.fourthline.cling.support.model.item.VideoItem
import org.opensilk.common.dagger.ActivityScope
import org.opensilk.common.dagger.Injector
import org.opensilk.common.dagger.ServiceScope
import org.opensilk.common.dagger.injectMe
import org.opensilk.common.loader.RxListLoader
import org.opensilk.common.loader.RxLoader
import org.opensilk.media.MediaMeta
import org.opensilk.media._setMediaMeta
import org.opensilk.media.toMediaItem
import org.opensilk.upnp.cds.browser.CDSUpnpService
import org.opensilk.upnp.cds.featurelist.BasicView
import org.opensilk.upnp.cds.featurelist.Features
import org.opensilk.upnp.cds.featurelist.XGetFeatureListCallback
import rx.Observable
import rx.Subscriber
import rx.subscriptions.Subscriptions
import timber.log.Timber
import java.io.IOException
import java.util.ArrayList
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Provider

/**
 * This module is superseded in mock for espresso tests
 */
@Module
abstract class UpnpLoadersModule {
    @Binds
    abstract fun provideCDSDevicesLoader(impl: CDSDevicesLoaderImpl): CDSDevicesLoader
    @Binds
    abstract fun provideCDSBrowseLoader(impl: CDSBrowseLoaderImpl): CDSBrowseLoader
}

/**
 * The Loader for the Media Servers row in the Home Activity
 */
interface CDSDevicesLoader: RxLoader<MediaBrowser.MediaItem>

/**
 * Created by drew on 5/29/17.
 */
@ActivityScope
class CDSDevicesLoaderImpl
@Inject constructor(
        private val mUpnpService: CDSUpnpService
): CDSDevicesLoader {
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
 * The loader for the folder activity
 */
interface CDSBrowseLoader: RxLoader<MediaBrowser.MediaItem>

/**
 *
 */
@ActivityScope
class CDSBrowseLoaderImpl
@Inject constructor(
        private val mUpnpService: CDSUpnpService,
        private val mMediaItem: MediaBrowser.MediaItem
) : CDSBrowseLoader {
    override val observable: Observable<MediaBrowser.MediaItem> by lazy {
        val mediaRef = newMediaRef(mMediaItem.mediaId)
        val folderId = when (mediaRef.kind) {
            UPNP_FOLDER -> mediaRef.mediaId as FolderId
            UPNP_DEVICE -> FolderId(mediaRef.mediaId.id, "0")
            else -> TODO("Unsupported mediaid")
        }
        return@lazy Observable.create(CDSOnSubscribe(mUpnpService, folderId))
                .flatMap { cdsservice -> Observable.create(BrowseOnSubscribe(mUpnpService, cdsservice,
                            folderId, BrowseFlag.DIRECT_CHILDREN)) }
    }

    fun createFeatureList(cdservice: RemoteService): Observable.OnSubscribe<String> {
        return Observable.OnSubscribe<String> { subscriber ->
            val callback = object : XGetFeatureListCallback(cdservice) {
                override fun received(actionInvocation: ActionInvocation<out Service<*, *>>?, features: Features?) {
                    if (subscriber.isUnsubscribed) {
                        return
                    }
                    features?.features?.firstOrNull { (it is BasicView && !it.videoItemId.isNullOrBlank()) }?.let {
                        sendNext((it as BasicView).videoItemId)
                        return
                    }
                    //the above failed send default root id
                    sendNext("0")
                }

                override fun failure(invocation: ActionInvocation<out Service<*, *>>?, operation: UpnpResponse?, defaultMsg: String?) {
                    sendNext("0")
                }

                private fun sendNext(n : String) {
                    if (subscriber.isUnsubscribed) {
                        return
                    }
                    subscriber.onNext(n)
                    subscriber.onCompleted()
                }
            }
        }
    }
}

private val CDSserviceType = UDAServiceType("ContentDirectory", 1)

/**
 * Fetches the content directory service from the server
 */
class CDSOnSubscribe
constructor(
        private val mUpnpService: CDSUpnpService,
        private val mFolderId: FolderId
) : Observable.OnSubscribe<RemoteService> {

    override fun call(subscriber: Subscriber<in RemoteService>) {
        val udn = UDN.valueOf(mFolderId.deviceId)
        //check cache first
        val rd = mUpnpService.registry.getRemoteDevice(udn, false)
        if (rd != null) {
            val rs = rd.findService(CDSserviceType)
            if (rs != null) {
                subscriber.onNext(rs)
                subscriber.onCompleted()
                return
            }
        }
        //missed cache, we have to look it up
        val listener = object : DefaultRegistryListener() {
            internal var once = AtomicBoolean(true)
            override fun remoteDeviceAdded(registry: Registry, device: RemoteDevice) {
                if (subscriber.isUnsubscribed) {
                    return
                }
                if (udn == device.identity.udn) {
                    val rs = device.findService(CDSserviceType)
                    if (rs != null) {
                        if (once.compareAndSet(true, false)) {
                            subscriber.onNext(rs)
                            subscriber.onCompleted()
                        } else {
                            Timber.w("Ignoring second notify of device $udn (${device.details.friendlyName})")
                        }
                    } else {
                        subscriber.onError(NoContentDirectoryFoundException())
                    }
                }
            }
        }
        //ensure we don't leak our listener
        subscriber.add(Subscriptions.create { mUpnpService.registry.removeListener(listener) })
        //register listener
        mUpnpService.registry.addListener(listener)
        Timber.d("Sending a new search for %s", udn)
        //upnpService.getControlPoint().search(new UDNHeader(udn));//doesnt work
        mUpnpService.controlPoint.search(UDAServiceTypeHeader(CDSserviceType))
    }
}

class NoContentDirectoryFoundException: Exception()

/**
 * performs the browse
 */
class BrowseOnSubscribe(
        private val mUpnpService: CDSUpnpService,
        private val mCDSService: RemoteService,
        private val mFolderId: FolderId,
        private val mBrowseFlag: BrowseFlag
) : Observable.OnSubscribe<MediaBrowser.MediaItem> {

    override fun call(subscriber: Subscriber<in MediaBrowser.MediaItem>) {
        val browse = object : Browse(mCDSService, mFolderId.folderId, mBrowseFlag) {
            override fun received(actionInvocation: ActionInvocation<*>, didl: DIDLContent) {
                if (subscriber.isUnsubscribed) {
                    return
                }

                val deviceId = mCDSService.device.identity.udn.identifierString

                for (c in didl.containers) {
                    val meta = MediaMeta()

                    meta.mediaId = MediaRef(UPNP_FOLDER, FolderId(deviceId, c.id)).toJson()
                    meta.mimeType = DocumentsContract.Document.MIME_TYPE_DIR

                    meta.title = c.title
                    c.childCount?.let {
                        meta.subtitle = "$it Children"
                    }

                    Timber.w("Item ${c.title} is of type ${c.clazz.friendlyName}")

                    subscriber.onNext(meta.toMediaItem())
                }

                for (item in didl.items) {
                    if (VideoItem.CLASS.equals(item)) {
                        val meta = MediaMeta()

                    } else {
                        Timber.i("Skipping not video item ${item.title}")
                    }
                }

                //TODO handle pagination
                val numRet = actionInvocation.getOutput("NumberReturned").value as UnsignedIntegerFourBytes
                val total = actionInvocation.getOutput("TotalMatches").value as UnsignedIntegerFourBytes
                if (numRet.value != 0L && total.value != 0L && numRet.value < total.value) {
                    Timber.e("TODO handle pagination")
                }
                // server was unable to compute total matches
                else if (numRet.value != 0L && total.value == 0L) {
                    //have to assume they sent us everything
                    subscriber.onCompleted()
                }
                // no results, total should return an error
                else if (numRet.value == 0L && total.value == 720L) {
                    //notify subscriber of no results
                    subscriber.onError(NoBrowseResultsException())
                } else {
                    subscriber.onError(Exception("Unhandled condition numRet = $numRet total = $total"))
                }
            }

            override fun updateStatus(status: Browse.Status) {
                //pass
            }

            override fun failure(invocation: ActionInvocation<*>, operation: UpnpResponse, defaultMsg: String) {
                if (!subscriber.isUnsubscribed) {
                    subscriber.onError(IOException(defaultMsg))
                }
            }
        }
        mUpnpService.controlPoint.execute(browse)
    }
}

/**
 *
 */
class NoBrowseResultsException: Exception()

/**
 *
 */
@ServiceScope
@Subcomponent
interface UpnpHolderServiceComponent: Injector<UpnpHolderService> {
    @Subcomponent.Builder
    abstract class Builder : Injector.Builder<UpnpHolderService>()
}

/**
 *
 */
@Module(subcomponents = arrayOf(UpnpHolderServiceComponent::class))
class UpnpHolderServiceModule

/**
 * Service that holds a reference to the upnpservice so it can be shutdown
 */
class UpnpHolderService: android.app.Service() {
    private val mBinder = HolderBinder()
    @Inject lateinit var mUpnpService: CDSUpnpService

    override fun onCreate() {
        super.onCreate()
        injectMe()
    }

    override fun onDestroy() {
        super.onDestroy()
        mUpnpService.shutdown()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return mBinder
    }

    class HolderBinder: Binder()
}