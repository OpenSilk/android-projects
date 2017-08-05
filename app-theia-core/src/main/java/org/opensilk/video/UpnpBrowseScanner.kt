package org.opensilk.video

import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import io.reactivex.disposables.Disposables
import io.reactivex.functions.Function
import org.fourthline.cling.model.action.ActionException
import org.fourthline.cling.model.message.header.UDAServiceTypeHeader
import org.fourthline.cling.model.meta.Device
import org.fourthline.cling.model.meta.Service
import org.fourthline.cling.model.types.UDN
import org.fourthline.cling.registry.DefaultRegistryListener
import org.fourthline.cling.registry.Registry
import org.fourthline.cling.support.contentdirectory.DIDLParser
import org.fourthline.cling.support.model.Protocol
import org.fourthline.cling.support.model.container.StorageFolder
import org.fourthline.cling.support.model.item.VideoItem
import org.opensilk.media.*
import org.opensilk.upnp.cds.browser.CDSBrowseAction
import org.opensilk.upnp.cds.browser.CDSUpnpService
import org.opensilk.upnp.cds.browser.CDSserviceType
import org.opensilk.upnp.cds.featurelist.BasicView
import timber.log.Timber
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Created by drew on 7/29/17.
 */
@Singleton
class UpnpBrowseScanner
@Inject constructor(
        private val mUpnpService: CDSUpnpService,
        private val mDatabaseClient: DatabaseClient
) {

    private val runningSessions = HashSet<Session>()

    inner class Session(val upnpDeviceId: UpnpDeviceId) {

        val mQueue = LinkedBlockingQueue<UpnpFolderId>()

        var sub: Disposable = Disposables.fromAction {
            synchronized(runningSessions) {
                runningSessions.remove(this)
            }
        }

        fun enqueue(folderId: UpnpFolderId) {
            mQueue.offer(folderId)
            mDatabaseClient.incrementUpnpDeviceScanning(UpnpDeviceId(folderId.deviceId))
        }

        fun runQueue(): Observable<UpnpFolderId> {
            return Observable.create<UpnpFolderId> { s ->
                s.setCancellable {
                    synchronized(runningSessions) {
                        runningSessions.remove(this)
                    }
                }
                while (!s.isDisposed) {
                    try {
                        val next = mQueue.poll(60, TimeUnit.SECONDS)
                        if (next != null) {
                            s.onNext(next)
                        }
                    } catch (e: InterruptedException) {
                        if (mQueue.isEmpty()) {
                            break
                        }
                    }
                }
                s.onComplete()
            }
        }

    }

    private class FolderWithMetaList(val folderId: UpnpFolderId, val list: List<MediaMeta>)

    fun scan(upnpDeviceId: UpnpDeviceId, updateId: Long) {
        val session = Session(upnpDeviceId)
        //cancel any previous scan sessions so we can start fresh
        synchronized(runningSessions) {
            runningSessions.firstOrNull { it.upnpDeviceId == upnpDeviceId }?.sub?.dispose()
            runningSessions.add(session)
        }
        session.enqueue(UpnpFolderId(upnpDeviceId.deviceId, "0"))
        session.sub = session.runQueue().subscribeOn(AppSchedulers.newThread).flatMapSingle { folderId ->
            return@flatMapSingle if (folderId.folderId == "0") {
                //for root folder, look for feature list, falling back to normal browse
                cachedService(folderId).flatMap { service ->
                    featureList(service, folderId).onErrorResumeNext(Function { browse(service, folderId) })
                }
            } else {
                //else just do browse
                cachedService(folderId).flatMap { service ->
                    browse(service, folderId)
                }
            }.toList().map { list ->
                //carry along the folder id with the list
                FolderWithMetaList(folderId, list)
            }
        }.subscribe({ fwl ->
            mDatabaseClient.hideChildrenOf(fwl.folderId)
            //loop the remote meta
            for (ext in fwl.list) {
                //insert/update remote item in database
                val ref = newMediaRef(ext.mediaId)
                when (ref.kind) {
                    UPNP_FOLDER -> {
                        mDatabaseClient.addUpnpFolder(ext)
                        //scan children as well
                        session.enqueue(ref.mediaId as UpnpFolderId)
                    }
                    UPNP_VIDEO -> mDatabaseClient.addUpnpVideo(ext)
                    else -> Timber.e("Invalid kind slipped through %s for %s", ref.kind, ext.displayName)
                }
            }
            mDatabaseClient.postChange(UpnpFolderChange(fwl.folderId))
            mDatabaseClient.decrementUpnpDeviceScanning(UpnpDeviceId(fwl.folderId.deviceId))
        }, { t ->
            mDatabaseClient.setUpnpDeviceSystemUpdateId(upnpDeviceId, 0)
            if (t is BrowseExceptionWithFolderId) {
                val tt = t.cause
                if (tt is ActionException) {
                    Timber.e(tt, "UpnpBrowseScanner: ActionException code=${tt.errorCode} msg=${tt.message}")
                    when (tt.errorCode) {
                        801 -> {
                            //TODO show user access denied
                        }
                    }
                } else {
                    Timber.e(tt, "UpnpBrowseScanner: msg=${tt?.message}")
                }
            } else {
                Timber.e(t, "UpnpBrowseScanner: msg=${t.message}")
            }
        }, {
            Timber.d("Scan complete for $upnpDeviceId")
            mDatabaseClient.setUpnpDeviceSystemUpdateId(upnpDeviceId, updateId)
        })
    }

    class BrowseExceptionWithFolderId(val folderId: UpnpFolderId, cause: Throwable): Exception(cause)

    /**
     * performs the browse
     */
    private fun browse(service: Service<*,*>, parentId: UpnpFolderId) : Observable<MediaMeta> {
        return Observable.create { subscriber ->
            val browse = CDSBrowseAction(mUpnpService.controlPoint, service, parentId.folderId)
            browse.run()
            if (subscriber.isDisposed){
                return@create
            }
            if (browse.error.get() != null) {
                subscriber.onError(BrowseExceptionWithFolderId(parentId, browse.error.get()))
                return@create
            }
            if (browse.result.get() == null) {
                subscriber.onError(BrowseExceptionWithFolderId(parentId, NullPointerException()))
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
                    if (StorageFolder.CLASS.equals(c)) {
                        subscriber.onNext(c.toMediaMeta(deviceId))
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
                        subscriber.onNext((item as VideoItem).toMediaMeta(deviceId))
                    } else {
                        Timber.w("Skipping unsupported item ${item.title}, type=${item.clazz.value}")
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
                subscriber.onError(BrowseExceptionWithFolderId(parentId, e))
            }
        }
    }

    /**
     * Fetches the cached service
     */
    private fun cachedService(parentId: UpnpFolderId): Observable<Service<*,*>> {
        return Observable.create { subscriber ->
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
            subscriber.onNext(rs)
            subscriber.onComplete()
        }
    }

    /**
     * Fetches the content directory service from the server
     */
    private fun service(parentId: UpnpFolderId): Observable<Service<*,*>> {
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
    private fun featureList(service: Service<*,*>, parentId: UpnpFolderId): Observable<MediaMeta> {
        return Single.create<String> { s ->
            val action = UpnpFeatureListAction(mUpnpService.controlPoint, service)
            action.run()
            if (s.isDisposed) {
                return@create
            }
            if (action.error.get() != null) {
                s.onError(action.error.get())
                return@create
            }
            action.features.get()?.features?.firstOrNull {
                it is BasicView && !it.videoItemId.isNullOrBlank()
            }?.let {
                s.onSuccess((it as BasicView).videoItemId)
            } ?: s.onError(NullPointerException())
        }.flatMapObservable { id ->
            browse(service, parentId.copy(folderId = id))
        }.map { meta ->
            val oldParent = newMediaRef(meta.parentMediaId)
            when (oldParent.kind) {
                UPNP_FOLDER -> {
                    meta.parentMediaId = oldParent.copy(mediaId = parentId).toJson()
                } //else TODO error
            }
            return@map meta
        }
    }

}