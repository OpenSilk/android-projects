/*
 * Copyright (c) 2016 OpenSilk Productions LLC.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.opensilk.upnp.cds.browser

import android.database.Cursor
import android.media.browse.MediaBrowser
import org.fourthline.cling.UpnpService
import org.fourthline.cling.android.AndroidUpnpService
import org.fourthline.cling.model.action.ActionInvocation
import org.fourthline.cling.model.message.UpnpResponse
import org.fourthline.cling.model.message.header.UDAServiceTypeHeader
import org.fourthline.cling.model.meta.RemoteDevice
import org.fourthline.cling.model.meta.RemoteService
import org.fourthline.cling.model.meta.Service
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
import org.fourthline.cling.support.model.item.MusicTrack
import org.opensilk.upnp.cds.featurelist.BasicView
import org.opensilk.upnp.cds.featurelist.Features
import org.opensilk.upnp.cds.featurelist.XGetFeatureListCallback
import rx.Observable
import rx.Subscriber
import rx.functions.Action0
import rx.subscriptions.Subscriptions
import timber.log.Timber
import java.io.IOException
import java.util.ArrayList
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Created by drew on 12/21/16.
 */
class CDSBrowser {

    companion object {
        internal val serviceType = UDAServiceType("ContentDirectory", 1)
    }

    //Fetches the content directory service from the server
    internal class ContentDirectoryOnSubscribe(val upnpService: AndroidUpnpService, val deviceIdentity: String) : Observable.OnSubscribe<RemoteService> {

        override fun call(subscriber: Subscriber<in RemoteService>) {
            val udn = UDN.valueOf(deviceIdentity)
            //check cache first
            val rd = upnpService.registry.getRemoteDevice(udn, false)
            if (rd != null) {
                val rs = rd.findService(serviceType)
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
                    if (udn == device.identity.udn) {
                        val rs = device.findService(serviceType)
                        if (rs != null && once.compareAndSet(true, false)) {
                            subscriber.onNext(rs)
                            subscriber.onCompleted()
                        }
                    }
                }
            }
            //ensure we don't leak our listener
            subscriber.add(Subscriptions.create { upnpService.registry.removeListener(listener) })
            //register listener
            upnpService.registry.addListener(listener)
            Timber.d("Sending a new search for %s", udn)
            //            upnpService.getControlPoint().search(new UDNHeader(udn));//doesnt work
            upnpService.controlPoint.search(UDAServiceTypeHeader(serviceType))
        }
    }

    //performs the browse
    internal class BrowseOnSubscribe(val mAuthority: String, val upnpService: AndroidUpnpService,
                                     val rs: RemoteService, val folderIdentity: String, val browseFlag: BrowseFlag) : Observable.OnSubscribe<Model> {

        override fun call(subscriber: Subscriber<in Model>) {
            val browse = object : Browse(rs, folderIdentity, browseFlag) {
                override fun received(actionInvocation: ActionInvocation<*>, didl: DIDLContent) {
                    if (!subscriber.isUnsubscribed) {
                        parseDidlContent(didl, actionInvocation, subscriber)
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
            upnpService.controlPoint.execute(browse)
        }

        fun parseDidlContent(didlContent: DIDLContent, actionInvocation: ActionInvocation<*>,
                             subscriber: Subscriber<in Model>) {
            val containers = didlContent.containers
            val items = didlContent.items
            val resources = ArrayList<Model>(containers.size + items.size)

            val deviceId = rs.device.identity.udn.identifierString

            for (c in didlContent.containers) {
                var b: Model? = null
                if (MusicArtist.CLASS.equals(c)) {
                    b = ModelUtil.parseArtist(mAuthority, deviceId, c as MusicArtist)
                } else if (MusicAlbum.CLASS.equals(c)) {
                    b = ModelUtil.parseAlbum(mAuthority, deviceId, c as MusicAlbum)
                } else {
                    b = ModelUtil.parseFolder(mAuthority, deviceId, c)
                }
                if (b != null) {
                    subscriber.onNext(b)
                }
            }

            for (item in items) {
                if (MusicTrack.CLASS.equals(item)) {
                    val mt = item as MusicTrack
                    val s = ModelUtil.parseSong(mAuthority, deviceId, mt)
                    if (s != null) {
                        subscriber.onNext(s)
                    }
                }
            }

            //TODO handle pagination
            val numRet = actionInvocation.getOutput("NumberReturned").value as UnsignedIntegerFourBytes
            val total = actionInvocation.getOutput("TotalMatches").value as UnsignedIntegerFourBytes
            // server was unable to compute total matches
            if (numRet.value !== 0 && total.value === 0) {
                if (containers.size != 0 && items.size != 0) {
                    //TODO
                }
            } else if (numRet.value === 0 && total.value === 720) {
                // no results, total should return an error
            }
            if (!subscriber.isUnsubscribed) {
                subscriber.onCompleted()
            }
        }
    }

    fun createFeatureList(cdservice: RemoteService): Observable.OnSubscribe<String> {
        return Observable.OnSubscribe<String> { subscriber ->
            subscriber!!
            object : XGetFeatureListCallback(cdservice) {
                override fun received(actionInvocation: ActionInvocation<out Service<*, *>>?, features: Features?) {
                    if (subscriber.isUnsubscribed) {
                        return
                    }
                    features?.features?.forEach {
                        if (it is BasicView) {
                            if (!it.videoItemId.isNullOrEmpty()) {
                                sendNext(it.videoItemId)
                                return
                            }
                        }
                    }
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