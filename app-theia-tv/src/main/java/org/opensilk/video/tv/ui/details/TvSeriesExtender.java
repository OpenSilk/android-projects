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

package org.opensilk.video.tv.ui.details;

import android.media.browse.MediaBrowser;
import android.support.v17.leanback.widget.Action;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.PresenterSelector;

import org.opensilk.common.dagger.ScreenScope;
import org.opensilk.video.data.DataService;
import org.opensilk.video.data.MediaMetaExtras;
import org.opensilk.video.tv.ui.common.MediaItemClickListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

import javax.inject.Inject;
import javax.inject.Named;

import rx.Observable;
import rx.Subscription;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.observables.ConnectableObservable;
import rx.subscriptions.CompositeSubscription;
import timber.log.Timber;

/**
 * Created by drew on 4/10/16.
 */
@ScreenScope
class TvSeriesExtender implements DetailsScreenExtender {
    final CompositeSubscription subscriptions = new CompositeSubscription();
    final DataService dataService;
    final ArrayObjectAdapter seriesAdapter;
    final Observable<MediaBrowser.MediaItem> mediaItemObservable;
    final PresenterSelector seasonSelector;
    final DetailsActionsAdapter actionsAdapter;

    ConnectableObservable<List<MediaBrowser.MediaItem>> listObservable;

    @Inject
    public TvSeriesExtender(
            DataService dataService,
            @Named("seriesRowAdapter") ArrayObjectAdapter seriesAdapter,
            @Named("mediaItem") Observable<MediaBrowser.MediaItem> mediaItemObservable,
            @Named("cardPresenterSelector") PresenterSelector seasonSelector,
            DetailsActionsAdapter actionsAdapter
    ) {
        this.dataService = dataService;
        this.seriesAdapter = seriesAdapter;
        this.mediaItemObservable = mediaItemObservable;
        this.seasonSelector = seasonSelector;
        this.actionsAdapter = actionsAdapter;
    }

    @Override
    public void onCreate(DetailsScreenFragment fragment) {
        listObservable = mediaItemObservable.flatMap(new Func1<MediaBrowser.MediaItem, Observable<List<MediaBrowser.MediaItem>>>() {
            @Override
            public Observable<List<MediaBrowser.MediaItem>> call(MediaBrowser.MediaItem item) {
                return dataService.getChildren(item);
            } //toSortedList wont work since we dont terminate.
        }).map(new Func1<List<MediaBrowser.MediaItem>, List<MediaBrowser.MediaItem>>() {
            @Override
            public List<MediaBrowser.MediaItem> call(List<MediaBrowser.MediaItem> list) {
                List<MediaBrowser.MediaItem> items = new ArrayList<>(list);
                Collections.sort(items, new Comparator<MediaBrowser.MediaItem>() {
                    @Override
                    public int compare(MediaBrowser.MediaItem left, MediaBrowser.MediaItem right) {
                        MediaMetaExtras leftE = MediaMetaExtras.from(left.getDescription());
                        MediaMetaExtras rightE = MediaMetaExtras.from(right.getDescription());
                        if (leftE.getSeasonNumber() == rightE.getSeasonNumber()) {
                            return leftE.getEpisodeNumber() - rightE.getEpisodeNumber();
                        } else {
                            return leftE.getSeasonNumber() - rightE.getSeasonNumber();
                        }
                    }
                });
                return items;
            }
        }).replay(1);
        addSeasons();
        subscriptions.add(listObservable.connect());
        fragment.setOnItemViewClickedListener(new MediaItemClickListener());
        Subscription actionSub = mediaItemObservable
                .subscribe(new Action1<MediaBrowser.MediaItem>() {
                    @Override
                    public void call(MediaBrowser.MediaItem item) {
                        updateActions(item);
                    }
                });
        subscriptions.add(actionSub);
    }

    @Override
    public void onDestroy(DetailsScreenFragment fragment) {
        subscriptions.clear();
    }

    void addSeasons() {
        Timber.d("addSeasons()");
        Subscription seasonNumSub = listObservable
                .subscribe(new Action1<List<MediaBrowser.MediaItem>>() {
                    @Override
                    public void call(List<MediaBrowser.MediaItem> list) {
                        Set<Integer> seasons = new TreeSet<>(); //auto sort
                        for (MediaBrowser.MediaItem mediaItem : list) {
                            MediaMetaExtras metaExtras = MediaMetaExtras.from(mediaItem.getDescription());
                            seasons.add(metaExtras.getSeasonNumber());
                        }
                        for (Integer season : seasons) {
                            addSeasonRow(season);
                        }
                    }
                });
        subscriptions.add(seasonNumSub);
    }

    void addSeasonRow(final int season) {
        Timber.d("addSeasonRow(%d)", season);
        if (season < 0) {
            addUnknownRow();
            return;
        }
        HeaderItem header = new HeaderItem(String.format(Locale.US, "Season %d", season));
        final ArrayObjectAdapter seasonAdapter = new ArrayObjectAdapter(seasonSelector);
        ListRow row = new ListRow(header, seasonAdapter);
        seriesAdapter.add(row);
        //TODO need to unsubscribe when parent adpter is cleared
        Subscription seasonSub = listObservable
                .subscribe(new Action1<List<MediaBrowser.MediaItem>>() {
                    @Override
                    public void call(List<MediaBrowser.MediaItem> list) {
                        for (MediaBrowser.MediaItem item : list) {
                            MediaMetaExtras metaExtras = MediaMetaExtras.from(item.getDescription());
                            if (metaExtras.getSeasonNumber() == season) {
                                seasonAdapter.add(item);
                            }
                        }
                    }
                });
        subscriptions.add(seasonSub);
    }

    void addUnknownRow() {
        Timber.w("addUnknownRow NOT IMPLEMENTED");
    }

    void updateActions(MediaBrowser.MediaItem mediaItem) {
        actionsAdapter.set(DetailsActions.PLAY, new Action(DetailsActions.PLAY, "Play (Ordered)"));
        actionsAdapter.set(DetailsActions.PLAY_RANDOM, new Action(DetailsActions.PLAY_RANDOM, "Play (Random)"));
    }

}
