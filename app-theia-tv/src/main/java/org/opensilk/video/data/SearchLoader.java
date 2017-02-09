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

package org.opensilk.video.data;

import android.content.ContentResolver;
import android.content.Context;
import android.media.browse.MediaBrowser;

import org.apache.commons.lang3.StringUtils;
import org.opensilk.common.core.rx.RxListLoader;
import org.opensilk.common.core.rx.RxUtils;

import java.util.List;

import rx.Observable;
import rx.Scheduler;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;

/**
 * Created by drew on 4/14/16.
 */
public abstract class SearchLoader implements RxListLoader<MediaBrowser.MediaItem> {

    static final Scheduler sSubscribeOn = Schedulers.io();
    static final Scheduler sObserveOn = AndroidSchedulers.mainThread();

    final ContentResolver mContentResolver;
    final VideosProviderClient mClient;

    final PublishSubject<List<MediaBrowser.MediaItem>> mBridge = PublishSubject.create();
    String mQuery;
    Subscription mSubscription;

    public SearchLoader(
            Context context,
            VideosProviderClient mClient
    ) {
        this.mContentResolver = context.getContentResolver();
        this.mClient = mClient;
    }

    public void setQuery(CharSequence query) {
        mQuery = StringUtils.isEmpty(query) ? "" : (query.toString() + "*");
    }

    /**
     * Replaces query and resubscribes
     * @param query
     */
    public void requery(CharSequence query) {
        setQuery(query);
        performSearch();
    }

    /**
     * @return Subject bridge, on subscribe will start query if set,
     * there is no need to resubscribe when changing query, use {@link #requery(CharSequence)}
     */
    @Override
    public Observable<List<MediaBrowser.MediaItem>> getListObservable() {
        return mBridge.doOnUnsubscribe(() -> {
            RxUtils.unsubscribe(mSubscription);
        }).doOnSubscribe(() -> {
            //We should only be subscribed to once so
            //im not gonna worry about multiple calls to this
            performSearch();
        });
    }

    void performSearch() {
        RxUtils.unsubscribe(mSubscription);
        if (StringUtils.isEmpty(mQuery)) {
            return;
        }
        mSubscription = makeObservable().toList()
                .subscribeOn(sSubscribeOn)
                .observeOn(sObserveOn)
                .subscribe(list -> {
                    mBridge.onNext(list);
                }, e -> {
                    mBridge.onError(e);
                }, () -> {
                    //pass
                });
    }

    protected abstract Observable<MediaBrowser.MediaItem> makeObservable();

}
