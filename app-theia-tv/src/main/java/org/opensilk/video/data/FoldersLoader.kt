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

import android.content.Context
import android.media.MediaDescription
import android.media.browse.MediaBrowser

import org.opensilk.common.dagger.ActivityScope
import org.opensilk.common.dagger.ForApplication
import org.opensilk.common.rx.RxListLoader
import org.opensilk.media.MediaMeta
import org.opensilk.media._setMediaMeta
import org.opensilk.video.R

import java.util.ArrayList

import javax.inject.Inject

import rx.Observable

/**
 * Created by drew on 4/10/16.
 */
@ActivityScope
class FoldersLoader
@Inject
constructor(@ForApplication internal val mContext: Context) : RxListLoader<MediaBrowser.MediaItem> {

    //never completes
    override fun getListObservable(): Observable<List<MediaBrowser.MediaItem>> {
        return Observable.create<List<MediaBrowser.MediaItem>> { subscriber ->
            val list = ArrayList<MediaBrowser.MediaItem>()
            list.add(networkItem)
            if (!subscriber.isUnsubscribed) {
                subscriber.onNext(list)
            }
        }
    }

    internal val networkItem: MediaBrowser.MediaItem
        get() {
            val builder = MediaDescription.Builder()
            val metaExtras = MediaMeta()
            metaExtras.mimeType = "vnd.opensilk.org/special"
            builder.setMediaId("special:network_folders")
            builder.setTitle("Local Network")
            metaExtras.artworkResourceId = R.drawable.server_network_48dp
            builder._setMediaMeta(metaExtras)
            return MediaBrowser.MediaItem(builder.build(), MediaBrowser.MediaItem.FLAG_BROWSABLE)
        }
}
