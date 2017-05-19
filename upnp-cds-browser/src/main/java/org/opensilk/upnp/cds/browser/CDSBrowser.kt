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

import android.media.browse.MediaBrowser
import org.fourthline.cling.UpnpService
import org.fourthline.cling.model.action.ActionInvocation
import org.fourthline.cling.model.message.UpnpResponse
import org.fourthline.cling.model.meta.RemoteDevice
import org.fourthline.cling.model.meta.RemoteService
import org.fourthline.cling.model.meta.Service
import org.opensilk.upnp.cds.featurelist.BasicView
import org.opensilk.upnp.cds.featurelist.Features
import org.opensilk.upnp.cds.featurelist.XGetFeatureListCallback
import rx.Observable
import rx.Subscriber

/**
 * Created by drew on 12/21/16.
 */
class CDSBrowser {

    private val mUpnpService: UpnpService

    fun getRoots(cdservice: RemoteService): rx.Observable<MediaBrowser.MediaItem> {
        Observable.create<MediaBrowser.MediaItem>(createFeatureList(cdservice))
    }

    fun createFeatureList(cdservice: RemoteService) {
        object : Observable.OnSubscribe<String> {
            override fun call(subscriber: Subscriber<in String>?) {
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

}