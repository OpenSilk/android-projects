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

    meta.title = this.title
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
        private val mDatabaseClient: DatabaseClient
): CDSDevicesLoader {

    override val observable: Observable<List<MediaBrowser.MediaItem>> by lazy {
        mDatabaseClient.changesObservable
                .filter { it is UpnpDeviceChange }
                .map { it as UpnpDeviceChange }
                .startWith(UpnpDeviceChange())
                .flatMap {
                    mDatabaseClient.getUpnpDevices()
                            .map { it.toMediaItem() }
                            .toList().subscribeOn(AppSchedulers.diskIo)
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
        private val mDatabaseClient: DatabaseClient,
        private val mUpnpBrowseService: UpnpBrowseService
) : CDSBrowseLoader {
    override fun observable(mediaId: String): Observable<MediaBrowser.MediaItem> {
        val mediaRef = newMediaRef(mediaId)
        val folderId = when (mediaRef.kind) {
            UPNP_FOLDER -> mediaRef.mediaId as UpnpFolderId
            UPNP_DEVICE -> UpnpFolderId((mediaRef.mediaId as UpnpDeviceId).deviceId, "0")
            else -> TODO("Unsupported mediaid")
        }
        mUpnpBrowseService.browse(folderId)
        return mDatabaseClient.changesObservable
                .filter { it is UpnpFolderChange && folderId == it.folderId }
                .flatMap<MediaMeta> {
                    Observable.concat(
                        mDatabaseClient.getUpnpFolders(folderId).subscribeOn(AppSchedulers.diskIo),
                        mDatabaseClient.getUpnpVideos(folderId).subscribeOn(AppSchedulers.diskIo)
                    )
                }.map { it.toMediaItem() }
    }
}

class NoContentDirectoryFoundException: Exception()

/**
 *
 */
class NoBrowseResultsException: Exception()
