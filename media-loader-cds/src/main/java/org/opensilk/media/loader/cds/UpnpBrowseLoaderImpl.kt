package org.opensilk.media.loader.cds

import io.reactivex.Observable
import io.reactivex.Single
import org.fourthline.cling.model.message.header.UDAServiceTypeHeader
import org.fourthline.cling.model.meta.Device
import org.fourthline.cling.model.meta.Service
import org.fourthline.cling.model.types.UDN
import org.fourthline.cling.registry.DefaultRegistryListener
import org.fourthline.cling.registry.Registry
import org.fourthline.cling.support.contentdirectory.DIDLParser
import org.fourthline.cling.support.model.Protocol
import org.fourthline.cling.support.model.container.*
import org.fourthline.cling.support.model.item.AudioItem
import org.fourthline.cling.support.model.item.MusicTrack
import org.fourthline.cling.support.model.item.VideoItem
import org.opensilk.media.*
import org.opensilk.upnp.cds.browser.CDSBrowseAction
import org.opensilk.upnp.cds.browser.CDSUpnpService
import org.opensilk.upnp.cds.browser.CDSserviceType
import org.opensilk.upnp.cds.featurelist.BasicView
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

/**
 * Default implementation of UpnpBrowseLoader
 *
 * Created by drew on 7/29/17.
 */
class UpnpBrowseLoaderImpl @Inject constructor(private val mUpnpService: CDSUpnpService): UpnpBrowseLoader {

    private data class Opts(val audio: Boolean, val video: Boolean)

    override fun directChildren(upnpFolderId: UpnpContainerId, wantVideoItems: Boolean,
                                wantAudioItems: Boolean): Single<out List<MediaRef>> {
        val opts = Opts(wantAudioItems, wantVideoItems)
        return if (upnpFolderId is UpnpDeviceId) {
            //for root folder, look for feature list, falling back to normal browse
            cachedService(upnpFolderId).flatMap { service ->
                featureList(service, upnpFolderId, opts)
                        .onErrorResumeNext(browse(service, upnpFolderId, opts))
                        .toList()
            }
        } else {
            //else just do browse
            cachedService(upnpFolderId).flatMap { service ->
                browse(service, upnpFolderId, opts)
                        .toList()
            }
        }
    }

    /**
     * performs the browse
     */
    private fun browse(service: Service<*, *>, parentId: UpnpContainerId, opts: Opts) : Observable<MediaRef> {
        return Observable.create { subscriber ->
            val browse = CDSBrowseAction(mUpnpService.controlPoint, service, parentId.containerId)
            browse.run()
            if (subscriber.isDisposed){
                return@create
            }
            if (browse.error.get() != null) {
                subscriber.onError(browse.error.get())
                return@create
            }
            if (browse.result.get() == null) {
                subscriber.onError(NullPointerException())
                return@create
            }
            val result = browse.result.get()
            if (result.countLong == 0L) {// && result.totalMatchesLong == 720L) {
                subscriber.onComplete()
                return@create
            }
            try {
                val didlParser = DIDLParser()
                val didl = didlParser.parse(result.result)
                val deviceId = UpnpDeviceId(parentId.deviceId)

                for (c in didl.containers) {
                    try {
                        if (StorageFolder.CLASS.equals(c)) {
                            subscriber.onNext((c as StorageFolder).toUpnpFolder(parentId))
                        } else if (MusicGenre.CLASS.equals(c) && opts.audio) {
                            subscriber.onNext((c as MusicGenre).toUpnpMusicGenre(parentId))
                        } else if (MusicAlbum.CLASS.equals(c) && opts.audio) {
                            subscriber.onNext((c as MusicAlbum).toUpnpMusicAlbum(parentId))
                        } else if (MusicArtist.CLASS.equals(c) && opts.audio) {
                            subscriber.onNext((c as MusicArtist).toUpnpMusicArtist(parentId))
                        } else {
                            Timber.w("Skipping unsupported container ${c.title} type ${c.clazz.value}")
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "Unable to parse ${c.title}")
                    }
                }

                for (item in didl.items) {
                    try {
                        if (item.clazz.value.startsWith("object.item.videoItem", true) && opts.video) {
                            val res = item.firstResource ?: continue
                            if (res.protocolInfo.protocol != Protocol.HTTP_GET) {
                                //we can only support http-get
                                Timber.w("Skipping item ${item.title} with unsupported resource protocol")
                                continue
                            }
                            subscriber.onNext((item as VideoItem).toMediaMeta(deviceId))
                        } else if (item.clazz.value.startsWith("object.item.audioItem", true) && opts.audio) {
                            val res = item.firstResource ?: continue
                            if (res.protocolInfo.protocol != Protocol.HTTP_GET) {
                                //we can only support http-get
                                Timber.w("Skipping item ${item.title} with unsupported resource protocol")
                                continue
                            }
                            if (MusicTrack.CLASS.equals(item)) {
                                subscriber.onNext((item as MusicTrack).toUpnpMusicTrack(parentId))
                            } else {
                                subscriber.onNext((item as AudioItem).toUpnpAudioTrack(parentId))
                            }
                        } else {
                            Timber.w("Skipping unsupported item ${item.title} class is ${item.clazz.value}")
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "Unable to parse ${item.title}")
                    }
                }

                if (result.countLong == result.totalMatchesLong) {
                    //they sent everything
                    subscriber.onComplete()
                } else {
                    //TODO handle pagination
                    subscriber.onComplete()
                }

            } catch (e: Exception) {
                subscriber.onError(e)
            }
        }
    }

    /**
     * Fetches the cached service
     */
    private fun cachedService(parentId: UpnpContainerId): Single<Service<*, *>> {
        return Single.create { subscriber ->
            val udn = UDN.valueOf(parentId.deviceId)
            //check cache first
            val rd = mUpnpService.registry.getDevice(udn, false)
            if (rd == null) {
                subscriber.onError(DeviceNotFoundException())
                return@create
            }
            val rs = rd.findService(CDSserviceType)
            if (rs == null) {
                subscriber.onError(ServiceNotFoundException())
                return@create
            }
            if (subscriber.isDisposed) {
                return@create
            }
            subscriber.onSuccess(rs)
        }
    }

    /**
     * Fetches the content directory service from the server
     */
    private fun service(parentId: UpnpFolderId): Observable<Service<*, *>> {
        return Observable.create { subscriber ->
            val udn = UDN.valueOf(parentId.deviceId)
            //missed cache, we have to look it up
            val listener = object : DefaultRegistryListener() {
                internal var once = AtomicBoolean(true)
                override fun deviceAdded(registry: Registry, device: Device<*, out Device<*, *, *>, out Service<*, *>>) {
                    if (subscriber.isDisposed) {
                        return
                    }
                    if (udn == device.identity.udn) {
                        val rs = device.findService(CDSserviceType)
                        if (rs != null) {
                            if (once.compareAndSet(true, false)) {
                                subscriber.onNext(rs)
                                subscriber.onComplete()
                            }
                        } else {
                            subscriber.onError(NoContentDirectoryFoundException())
                        }
                    }
                }
            }
            //register listener
            mUpnpService.registry.addListener(listener)
            //ensure we don't leak our listener
            subscriber.setCancellable { mUpnpService.registry.removeListener(listener) }
            mUpnpService.controlPoint.search(UDAServiceTypeHeader(CDSserviceType))
        }
    }

    /**
     * featureList uses proprietary action to fetch the virtual folder with only video items
     * it then remaps the children of that folder to the root container (id = "0")
     * so the loader sees the video folders when requesting root
     */
    private fun featureList(service: Service<*, *>, deviceId: UpnpDeviceId, opts: Opts): Observable<MediaRef> {
        return Single.create<String> { s ->
            if (opts.audio && opts.video) {
                //oops they want both
                s.onError(FeatureListException())
                return@create
            }
            val action = UpnpFeatureListAction(mUpnpService.controlPoint, service)
            action.run()
            if (s.isDisposed) {
                return@create
            }
            if (action.error.get() != null) {
                s.onError(action.error.get())
                return@create
            }
            val features = action.features.get()?.features?.firstOrNull { it is BasicView } as? BasicView
            val videoId = features?.videoItemId ?: ""
            val audioId = features?.audioItemId ?: ""
            if (videoId.isNotBlank() && opts.video) {
                s.onSuccess(videoId)
            } else if (!audioId.isNotBlank() && opts.audio) {
                s.onSuccess(audioId)
            } else {
                s.onError(FeatureListException())
            }
        }.flatMapObservable { id ->
            //do browse, remaping the parent id to the root id
            browse(service, UpnpFolderId(deviceId.deviceId, id), opts).map { meta ->
                return@map when (meta) {
                    is UpnpAudioRef -> meta.copy(parentId = deviceId)
                    is UpnpFolderRef -> meta.copy(parentId = deviceId)
                    is UpnpMusicAlbumRef -> meta.copy(parentId = deviceId)
                    is UpnpMusicArtistRef -> meta.copy(parentId = deviceId)
                    is UpnpMusicGenreRef -> meta.copy(parentId = deviceId)
                    is UpnpMusicTrackRef -> meta.copy(parentId = deviceId)
                    is UpnpVideoRef -> meta.copy(parentId = deviceId)
                    else -> TODO("need to add ${meta::javaClass.name} to the switch")
                }
            }.switchIfEmpty {
                //just in case we were given bad ids by featurelist
                Observable.error<MediaRef>(FeatureListException())
            }
        }
    }

}