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

package org.opensilk.video.data

import android.media.MediaDescription
import android.media.browse.MediaBrowser
import android.net.Uri

import org.apache.commons.lang3.StringUtils
import org.opensilk.common.dagger.ActivityScope
import org.opensilk.common.loader.RxLoader
import org.opensilk.video.playback.VLCInstance
import org.videolan.libvlc.Media

import javax.inject.Inject

import rx.Observable
import rx.functions.Func1
import timber.log.Timber

/**
 * Created by drew on 4/10/16.
 */
@ActivityScope
class UpnpDevicesLoader
@Inject
constructor(internal val mVlcInstance: VLCInstance) : RxLoader<MediaBrowser.MediaItem> {

    override val observable: Observable<MediaBrowser.MediaItem>
        get() =  Observable.using<Media, org.videolan.libvlc.util.MediaBrowser>(
                { org.videolan.libvlc.util.MediaBrowser(mVlcInstance.get(), null) },
                { browser ->
                    Observable.create<Media> { subscriber ->
                        browser.changeEventListener(object : VLCBrowserEventListener() {
                            override fun onMediaAdded(i: Int, media: Media) {
                                if (!subscriber.isUnsubscribed) {
                                    subscriber.onNext(media)
                                }
                            }

                            override fun onBrowseEnd() {
                                Timber.d("onBrowseEnd()")
                                if (!subscriber.isUnsubscribed) {
                                    subscriber.onCompleted()
                                }
                            }
                        })
                        browser.discoverNetworkShares("upnp")
                    }
                }, org.videolan.libvlc.util.MediaBrowser::release
        ).map<MediaBrowser.MediaItem>(Func1 { media ->
            val title = media.getMeta(Media.Meta.Title)
            val artworkUrl = media.getMeta(Media.Meta.ArtworkURL)
            val metaExtras = MediaMetaExtras.directory()
            metaExtras.serverId = media.getMeta(Media.Meta.Description)
            metaExtras.setMediaTitle(title)
            val builder = MediaDescription.Builder()
                    .setTitle(title)
                    .setMediaId("directory:" + media.uri)
            MediaDescriptionUtil.setMediaUri(builder, metaExtras, media.uri)
            builder.setExtras(metaExtras.bundle)
            if (!StringUtils.isEmpty(artworkUrl)) {
                builder.setIconUri(Uri.parse(artworkUrl))
            }
            MediaBrowser.MediaItem(builder.build(), MediaBrowser.MediaItem.FLAG_BROWSABLE)
        })

}
