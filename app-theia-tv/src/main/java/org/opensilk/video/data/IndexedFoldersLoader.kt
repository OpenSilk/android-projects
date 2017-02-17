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
import android.database.ContentObserver
import android.media.browse.MediaBrowser
import android.net.Uri

import org.opensilk.common.dagger.ActivityScope
import org.opensilk.common.dagger.ForApplication
import org.opensilk.common.rx.RxListLoader
import java.util.concurrent.TimeUnit

import javax.inject.Inject

import rx.Observable
import rx.Scheduler
import rx.Subscription
import rx.android.schedulers.AndroidSchedulers
import rx.functions.Action1
import rx.schedulers.Schedulers
import rx.subscriptions.Subscriptions
import timber.log.Timber

/**
 * Created by drew on 4/10/16.
 */
@ActivityScope
class IndexedFoldersLoader
@Inject
constructor(
        @ForApplication internal val mContext: Context,
        internal val mClient: VideosProviderClient,
        internal val mDataService: DataService
) : RxListLoader<MediaBrowser.MediaItem> {

    override fun getListObservable(): Observable<List<MediaBrowser.MediaItem>> {
        return makeObservable().subscribeOn(sSubscribeOn).observeOn(sObserveOn)
    }

    //never completes
    private fun makeObservable(): Observable<List<MediaBrowser.MediaItem>> {
        return Observable.create<List<MediaBrowser.MediaItem>> { subscriber ->
            val items = mClient.topLevelDirectories
            if (subscriber.isUnsubscribed) {
                return@create
            }
            subscriber.onNext(items)
            //            final Action1<Uri> onChangeAction = uri -> {
            //                List<MediaBrowser.MediaItem> items1 = mClient.getTopLevelDirectories();
            //                if (subscriber.isUnsubscribed()) {
            //                    return;
            //                }
            //                subscriber.onNext(items1);
            //            };
            //            //Nobody notifies us of changes so we have to watch all our children
            //            for (MediaBrowser.MediaItem mediaItem : items) {
            //                Subscription s = mDataService.getMediaUriChanges(
            //                        MediaItemUtil.getMediaUri(mediaItem)).subscribe(onChangeAction);
            //                subscriber.add(s);
            //            }
        }
    }

    companion object {
        internal val sSubscribeOn = Schedulers.io()
        internal val sObserveOn = AndroidSchedulers.mainThread()
    }

}
