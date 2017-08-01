package org.opensilk.video

import org.fourthline.cling.model.action.ActionException
import org.fourthline.cling.model.action.ActionInvocation
import org.fourthline.cling.model.message.UpnpResponse
import org.fourthline.cling.model.message.header.UDAServiceTypeHeader
import org.fourthline.cling.model.meta.Device
import org.fourthline.cling.model.meta.RemoteService
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
import org.opensilk.upnp.cds.featurelist.Features
import org.opensilk.upnp.cds.featurelist.XGetFeatureListCallback
import rx.Observable
import rx.lang.kotlin.PublishSubject
import rx.subscriptions.Subscriptions
import timber.log.Timber
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

    private val mQueueSubject = PublishSubject<UpnpFolderId>()
    private val mStarted = AtomicBoolean(false)

    fun enqueue(folderId: UpnpFolderId) {
        if (mStarted.compareAndSet(false, true)) {
            subscribe()
        }
        mQueueSubject.onNext(folderId)
    }

    private class FolderWithMetaList(val folderId: UpnpFolderId, val list: List<MediaMeta>)

    //TODO use containerUpdateId and don't scan if no change
    private fun subscribe() {
        mQueueSubject.onBackpressureBuffer().observeOn(AppSchedulers.scanner).flatMap { folderId ->
            cachedService(folderId).flatMap { service -> browse(service, folderId) }
                    .toList().map { list -> FolderWithMetaList(folderId, list) }
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
                        mQueueSubject.onNext(ref.mediaId as UpnpFolderId)
                    }
                    UPNP_VIDEO -> mDatabaseClient.addUpnpVideo(ext)
                    else -> Timber.e("Invalid kind slipped through %s for %s", ref.kind, ext.displayName)
                }
            }
            //TODO remove still hidden items ?? for now just leaving
            mDatabaseClient.postChange(UpnpFolderChange(fwl.folderId))
        }, { t ->
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
            if (subscriber.isUnsubscribed){
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
                subscriber.onCompleted()
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
                    subscriber.onCompleted()
                } else {
                    //TODO handle pagination
                    subscriber.onCompleted()
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
            if (subscriber.isUnsubscribed) {
                return@create
            }
            subscriber.onNext(rs)
            subscriber.onCompleted()
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
                    if (subscriber.isUnsubscribed) {
                        return
                    }
                    if (udn == device.identity.udn) {
                        val rs = device.findService(CDSserviceType)
                        if (rs != null) {
                            if (once.compareAndSet(true, false)) {
                                subscriber.onNext(rs)
                                subscriber.onCompleted()
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
            subscriber.add(Subscriptions.create { mUpnpService.registry.removeListener(listener) })
            mUpnpService.controlPoint.search(UDAServiceTypeHeader(CDSserviceType))
        }
    }

    private fun featureList(cdservice: RemoteService): Observable.OnSubscribe<String> {
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

                override fun failure(invocation: ActionInvocation<out Service<*, *>>?,
                                     operation: UpnpResponse?, defaultMsg: String?) {
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
            mUpnpService.controlPoint.execute(callback)
        }
    }

}