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

import android.content.ContentResolver
import android.content.Context
import android.media.browse.MediaBrowser

import org.apache.commons.lang3.StringUtils
import org.opensilk.common.loader.RxListLoader
import org.opensilk.common.rx.RxUtils

import rx.Observable
import rx.Scheduler
import rx.Subscription
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import rx.subjects.PublishSubject
import rx.subscriptions.Subscriptions

/**
 * Created by drew on 4/14/16.
 */
abstract class SearchLoader(
        context: Context,
        internal val mClient: VideosProviderClient
) : RxListLoader<MediaBrowser.MediaItem> {

    internal val mContentResolver: ContentResolver

    internal val mBridge: PublishSubject<List<MediaBrowser.MediaItem>> = PublishSubject.create<List<MediaBrowser.MediaItem>>()
    internal var mQuery: String = ""
    internal var mSubscription: Subscription = Subscriptions.empty()

    init {
        this.mContentResolver = context.contentResolver
    }

    fun setQuery(query: CharSequence) {
        mQuery = if (StringUtils.isEmpty(query)) "" else query.toString() + "*"
    }

    /**
     * Replaces query and resubscribes
     * @param query
     */
    fun requery(query: CharSequence) {
        setQuery(query)
        performSearch()
    }

    /**
     * @return Subject bridge, on subscribe will start query if set,
     * * there is no need to resubscribe when changing query, use [.requery]
     */
    override val listObservable: Observable<List<MediaBrowser.MediaItem>>
        get() = mBridge.doOnUnsubscribe { mSubscription.unsubscribe() }.doOnSubscribe {
            //We should only be subscribed to once so
            //im not gonna worry about multiple calls to this
            performSearch()
        }

    internal fun performSearch() {
        mSubscription.unsubscribe()
        if (mQuery.isNullOrEmpty()) {
            return
        }
        mSubscription = makeObservable().toList()
                .subscribeOn(sSubscribeOn)
                .observeOn(sObserveOn)
                .subscribe(
                        { list -> mBridge.onNext(list) },
                        { e -> mBridge.onError(e) },
                        { /*pass*/ }
                )
    }

    protected abstract fun makeObservable(): Observable<MediaBrowser.MediaItem>

    companion object {

        internal val sSubscribeOn = Schedulers.io()
        internal val sObserveOn = AndroidSchedulers.mainThread()
    }

}
