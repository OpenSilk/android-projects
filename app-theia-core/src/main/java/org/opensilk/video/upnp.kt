package org.opensilk.video

import android.content.Context
import android.database.ContentObserver
import android.media.browse.MediaBrowser
import android.net.Uri
import android.os.Handler
import android.provider.DocumentsContract
import dagger.Binds
import dagger.Module
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
import org.fourthline.cling.support.model.Protocol
import org.fourthline.cling.support.model.container.Container
import org.fourthline.cling.support.model.container.StorageFolder
import org.fourthline.cling.support.model.item.VideoItem
import org.opensilk.common.dagger.ForApplication
import org.opensilk.media.*
import org.opensilk.upnp.cds.browser.CDSUpnpService
import org.opensilk.upnp.cds.browser.CDSserviceType
import org.opensilk.upnp.cds.featurelist.BasicView
import org.opensilk.upnp.cds.featurelist.Features
import org.opensilk.upnp.cds.featurelist.XGetFeatureListCallback
import rx.Observable
import rx.Subscriber
import rx.android.schedulers.AndroidSchedulers
import rx.android.schedulers.HandlerScheduler
import rx.subscriptions.Subscriptions
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject


fun Device<*,*,*>.toMediaMeta(): MediaMeta {
    val device = this
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

fun Container.toMediaMeta(deviceId: UpnpDeviceId): MediaMeta {
    val meta = MediaMeta()

    meta.mediaId = MediaRef(UPNP_FOLDER, UpnpFolderId(deviceId.deviceId, this.id)).toJson()
    meta.parentMediaId = MediaRef(UPNP_FOLDER, UpnpFolderId(deviceId.deviceId, this.parentID)).toJson()
    meta.mimeType = DocumentsContract.Document.MIME_TYPE_DIR

    meta.displayName = this.title
    this.childCount?.let {
        meta.subtitle = "$it Children"
    }
    return meta
}

fun VideoItem.toMediaMeta(deviceId: UpnpDeviceId): MediaMeta {
    val meta = MediaMeta()
    meta.mediaId = MediaRef(UPNP_VIDEO, UpnpVideoId(deviceId.deviceId, this.id)).toJson()
    meta.parentMediaId = MediaRef(UPNP_FOLDER, UpnpFolderId(deviceId.deviceId, this.parentID)).toJson()
    val res = this.firstResource
    meta.mediaUri = Uri.parse(res.value)
    meta.mimeType = res.protocolInfo.contentFormat
    meta.duration = parseUpnpDuration(res.duration)
    meta.bitrate = res.bitrate ?: 0L
    meta.size = res.size ?: 0L
    meta.resolution = res.resolution
    meta.displayName = this.title
    return meta
}

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
interface CDSDevicesLoader {
    val observable: Observable<List<MediaBrowser.MediaItem>>
}

/**
 * Created by drew on 5/29/17.
 */
class CDSDevicesLoaderImpl
@Inject constructor(
        @ForApplication private val mContext: Context,
        private val mDatabaseClient: DatabaseClient
): CDSDevicesLoader {

    override val observable: Observable<List<MediaBrowser.MediaItem>> by lazy {
        Observable.create<Boolean> { s ->
            val co = object: ContentObserver(null) {
                override fun onChange(selfChange: Boolean, uri: Uri?) {
                    s.onNext(!selfChange)
                }
            }
            mContext.contentResolver.registerContentObserver(mDatabaseClient.uris.upnpDevices(), true, co)
            s.add(Subscriptions.create { mContext.contentResolver.unregisterContentObserver(co) })
        }.startWith(true).flatMap {
            mDatabaseClient.getUpnpDevices().map { it.toMediaItem() }.toList().subscribeOn(AppSchedulers.diskIo)
        }
    }
}

/**
 * The loader for the folder activity
 */
interface CDSBrowseLoader {
    fun observable(mediaId: String): Observable<MediaBrowser.MediaItem>
}

/**
 *
 */
class CDSBrowseLoaderImpl
@Inject constructor(
        private val mUpnpService: CDSUpnpService
) : CDSBrowseLoader {
    override fun observable(mediaId: String): Observable<MediaBrowser.MediaItem> {
        val mediaRef = newMediaRef(mediaId)
        val folderId = when (mediaRef.kind) {
            UPNP_FOLDER -> mediaRef.mediaId as UpnpFolderId
            UPNP_DEVICE -> UpnpFolderId((mediaRef.mediaId as UpnpDeviceId).deviceId, "0")
            else -> TODO("Unsupported mediaid")
        }
        return Observable.create(CDSOnSubscribe(mUpnpService, folderId))
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


/**
 * Fetches the content directory service from the server
 */
class CDSOnSubscribe
constructor(
        private val mUpnpService: CDSUpnpService,
        private val mFolderId: UpnpFolderId
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
        private val mFolderId: UpnpFolderId,
        private val mBrowseFlag: BrowseFlag
) : Observable.OnSubscribe<MediaBrowser.MediaItem> {

    override fun call(subscriber: Subscriber<in MediaBrowser.MediaItem>) {
        val browse = object : Browse(mCDSService, mFolderId.folderId, mBrowseFlag) {
            override fun received(actionInvocation: ActionInvocation<*>, didl: DIDLContent) {
                Timber.d("BrowseOnSubscribe.received(${actionInvocation.action}, count=${didl.count}")
                if (subscriber.isUnsubscribed) {
                    return
                }

                val deviceId = UpnpDeviceId(mFolderId.deviceId)

                for (c in didl.containers) {
                    if (StorageFolder.CLASS.equals(c)) {
                        subscriber.onNext(c.toMediaMeta(deviceId).toMediaItem())
                    } else {
                        Timber.w("Skipping unsupported container ${c.title} type ${c.clazz.value}")
                    }
                }

                for (item in didl.items) {
                    if (VideoItem.CLASS.equals(item)) {
                        val res = item.firstResource ?: continue
                        if (res.protocolInfo.protocol != Protocol.HTTP_GET) {
                            //we can only support http-get
                            continue
                        }
                        subscriber.onNext((item as VideoItem).toMediaMeta(deviceId).toMediaItem())
                    } else {
                        Timber.w("Skipping unsupported item ${item.title}, type=${item.clazz.value}")
                    }
                }

                //TODO handle pagination
                val numRet = actionInvocation.getOutput("NumberReturned").value as UnsignedIntegerFourBytes
                val total = actionInvocation.getOutput("TotalMatches").value as UnsignedIntegerFourBytes
                if (numRet.value == total.value) {
                    //they sent everything
                    subscriber.onCompleted()
                }
                // no results, total should return an error
                else if (numRet.value == 0L && total.value == 720L) {
                    //notify subscriber of no results
                    subscriber.onError(NoBrowseResultsException())
                }
                // server was unable to compute total matches
                else if (numRet.value != 0L && total.value != 0L && numRet.value < total.value) {
                    Timber.e("TODO handle pagination")
                }
                else {
                    //server reported something weird, just ignore them
                    Timber.w("Server sent strange response numRet=$numRet, total=$total")
                    subscriber.onCompleted()
                }
            }

            override fun updateStatus(status: Browse.Status) {
                //pass
            }

            override fun failure(invocation: ActionInvocation<*>, operation: UpnpResponse, defaultMsg: String) {
                if (!subscriber.isUnsubscribed) {
                    subscriber.onError(invocation.failure)
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