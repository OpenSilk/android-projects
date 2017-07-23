package org.opensilk.video

import android.media.browse.MediaBrowser
import org.fourthline.cling.model.action.ActionInvocation
import org.fourthline.cling.model.message.UpnpResponse
import org.fourthline.cling.model.message.header.UDAServiceTypeHeader
import org.fourthline.cling.model.meta.Device
import org.fourthline.cling.model.meta.RemoteDevice
import org.fourthline.cling.model.meta.RemoteService
import org.fourthline.cling.model.meta.Service
import org.fourthline.cling.model.types.UDN
import org.fourthline.cling.model.types.UnsignedIntegerFourBytes
import org.fourthline.cling.registry.DefaultRegistryListener
import org.fourthline.cling.registry.Registry
import org.fourthline.cling.support.contentdirectory.callback.Browse
import org.fourthline.cling.support.model.BrowseFlag
import org.fourthline.cling.support.model.DIDLContent
import org.fourthline.cling.support.model.Protocol
import org.fourthline.cling.support.model.container.StorageFolder
import org.fourthline.cling.support.model.item.VideoItem
import org.opensilk.media.*
import org.opensilk.upnp.cds.browser.CDSUpnpService
import org.opensilk.upnp.cds.browser.CDSserviceType
import org.opensilk.upnp.cds.featurelist.BasicView
import org.opensilk.upnp.cds.featurelist.Features
import org.opensilk.upnp.cds.featurelist.XGetFeatureListCallback
import rx.Observable
import rx.Subscriber
import rx.subscriptions.Subscriptions
import timber.log.Timber
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Created by drew on 7/23/17.
 */
@Singleton
class UpnpBrowseService
@Inject constructor(
        private val mUpnpService: CDSUpnpService,
        private val mDatabaseClient: DatabaseClient
) {

    private val mBrowseFlag = BrowseFlag.DIRECT_CHILDREN

    fun browse(parentId: UpnpFolderId) {
        Observable.zip(
                getCDSService(parentId).flatMap { browseObservable(it) }.toList(),
                Observable.merge(
                        mDatabaseClient.getUpnpFolders(parentId).subscribeOn(AppSchedulers.diskIo),
                        mDatabaseClient.getUpnpVideos(parentId).subscribeOn(AppSchedulers.diskIo)
                ).toList(),
                { r, l -> ListHolder(LinkedList(r), LinkedList(l)) }
        ).subscribe( { lh ->
            //loop the remote meta
            for (ext in lh.remote) {
                //remove from local list
                val loc = lh.local.find { it.mediaId == ext.mediaId }
                if (loc != null) {
                    lh.local.remove(loc)
                }
                //insert/update remote item in database
                val ref = newMediaRef(ext.mediaId)
                when (ref.kind) {
                    UPNP_FOLDER -> mDatabaseClient.addUpnpFolder(ext)
                    UPNP_VIDEO -> mDatabaseClient.addUpnpVideo(ext)
                    else -> Timber.e("Invalid kind slipped through %s for %s", ref.kind, ext.displayName)
                }
            }
            //remove items in database no longer on remote
            for (rem in lh.local) {
                val ref = newMediaRef(rem.mediaId)
                when (ref.kind) {
                    UPNP_FOLDER -> mDatabaseClient.removeUpnpFolder(rem.rowId)
                    UPNP_VIDEO -> mDatabaseClient.removeUpnpVideo(rem.rowId)
                    else -> Timber.e("Invalid kind slipped through %s for %s", ref.kind, rem.displayName)
                }
            }
            mDatabaseClient.postChange(UpnpFolderChange(parentId))
        })
    }

    class ListHolder(val remote: LinkedList<MediaMeta>, val local: LinkedList<MediaMeta>)

    /**
     * performs the browse
     */
    fun browseObservable(holder: ParentWithService) : Observable<MediaMeta> {
        return Observable.create { subscriber ->
            val browse = object : Browse(holder.service, holder.parentId.folderId, mBrowseFlag) {
                override fun received(actionInvocation: ActionInvocation<*>, didl: DIDLContent) {
                    Timber.d("BrowseOnSubscribe.received(${actionInvocation.action}, count=${didl.count}")
                    if (subscriber.isUnsubscribed) {
                        return
                    }

                    val deviceId = UpnpDeviceId(holder.parentId.deviceId)

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
                        subscriber.onCompleted()
                    }
                    //server reported something weird, just ignore them
                    else {
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
     * Fetches the content directory service from the server
     */
    fun getCDSService(parentId: UpnpFolderId): Observable<ParentWithService> {
        return Observable.create { subscriber ->
            val udn = UDN.valueOf(parentId.deviceId)
            //check cache first
            val rd = mUpnpService.registry.getDevice(udn, false)
            if (rd != null) {
                val rs = rd.findService(CDSserviceType)
                if (rs != null) {
                    subscriber.onNext(ParentWithService(parentId, rs))
                    subscriber.onCompleted()
                    return@create
                }
            }
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
                                subscriber.onNext(ParentWithService(parentId, rs))
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

    class ParentWithService(val parentId: UpnpFolderId, val service: Service<*,*>)

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