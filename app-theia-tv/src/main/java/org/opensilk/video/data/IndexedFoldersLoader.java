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

import android.content.Context;
import android.database.ContentObserver;
import android.media.browse.MediaBrowser;
import android.net.Uri;

import org.opensilk.common.core.dagger2.ActivityScope;
import org.opensilk.common.core.dagger2.ForApplication;
import org.opensilk.common.core.rx.RxListLoader;

import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import rx.Observable;
import rx.Scheduler;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import rx.subscriptions.Subscriptions;
import timber.log.Timber;

/**
 * Created by drew on 4/10/16.
 */
@ActivityScope
public class IndexedFoldersLoader implements RxListLoader<MediaBrowser.MediaItem> {

    final Context mContext;
    final VideosProviderClient mClient;
    final DataService mDataService;
    static final Scheduler sSubscribeOn = Schedulers.io();
    static final Scheduler sObserveOn = AndroidSchedulers.mainThread();

    @Inject
    public IndexedFoldersLoader(
            @ForApplication Context mContext,
            VideosProviderClient mClient,
            DataService mDataService
    ) {
        this.mContext = mContext;
        this.mClient = mClient;
        this.mDataService = mDataService;
    }

    @Override
    public Observable<List<MediaBrowser.MediaItem>> getListObservable() {
        return makeObservable().subscribeOn(sSubscribeOn).observeOn(sObserveOn);
    }

    //never completes
    private Observable<List<MediaBrowser.MediaItem>> makeObservable() {
        return Observable.create(subscriber -> {
            List<MediaBrowser.MediaItem> items = mClient.getTopLevelDirectories();
            if (subscriber.isUnsubscribed()) {
                return;
            }
            subscriber.onNext(items);
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
        });
    }

}
