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

import android.content.Context;
import android.media.MediaDescription;
import android.media.browse.MediaBrowser;
import android.net.Uri;
import android.support.v17.leanback.widget.ClassPresenterSelector;
import android.support.v17.leanback.widget.DetailsOverviewRow;
import android.support.v17.leanback.widget.FullWidthDetailsOverviewRowPresenter;
import android.support.v17.leanback.widget.ObjectAdapter;
import android.support.v17.leanback.widget.OnActionClickedListener;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.PresenterSelector;

import org.opensilk.common.core.dagger2.ForActivity;
import org.opensilk.common.core.dagger2.ScreenScope;
import org.opensilk.video.data.DataService;
import org.opensilk.video.data.MediaItemUtil;
import org.opensilk.video.data.MediaMetaExtras;
import org.opensilk.video.data.VideoDescInfo;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Named;

import dagger.Module;
import dagger.Provides;
import rx.subscriptions.CompositeSubscription;
import timber.log.Timber;

/**
 * Created by drew on 3/16/16.
 */
@Module
public class DetailsScreenModule {

    private final Context activityContext;

    public DetailsScreenModule(Context activityContext) {
        this.activityContext = activityContext;
    }

    @Provides @ForActivity
    public Context provideActivityContext() {
        return activityContext;
    }

    @Provides @ScreenScope
    public MediaDescription provideMediaDescription(MediaBrowser.MediaItem mediaItem) {
        return mediaItem.getDescription();
    }

    @Provides @ScreenScope @Named("mediaItem")
    public rx.Observable<MediaBrowser.MediaItem> provideMediaItemObservable(
            DataService dataService, MediaBrowser.MediaItem mediaItem
    ) {
        return dataService.getMediaItem(mediaItem)
                .onErrorReturn(throwable -> {
                    Timber.e(throwable, "Failure retrieving mediaItem %s",
                            MediaItemUtil.getMediaTitle(mediaItem));
                    return mediaItem;
                })
                .replay(1).refCount();
    }

    @Provides @ScreenScope @Named("iconImage")
    public rx.Observable<Uri> provideIconImageObservable(
            @Named("mediaItem") rx.Observable<MediaBrowser.MediaItem> mediaItemObservable
    ) {
        return mediaItemObservable.map(item -> item.getDescription().getIconUri());
    }

    @Provides @ScreenScope @Named("backgroundImage")
    public rx.Observable<Uri> provideBackgroundImageUri(
            @Named("mediaItem") rx.Observable<MediaBrowser.MediaItem> mediaItemObservable
    ) {
        return mediaItemObservable.map(item -> {
            MediaMetaExtras metaExtras = MediaMetaExtras.from(item.getDescription());
            return metaExtras.getBackdropUri();
        });
    }

    @Provides @ScreenScope @Named("detailsOverview")
    public rx.Observable<VideoDescInfo> provideVideoDescChanges(
            @Named("mediaItem") rx.Observable<MediaBrowser.MediaItem> mediaItemObservable,
            DataService dataService

    ) {
        //overview is not included in mediaItem so we must fetch from database
        return mediaItemObservable.flatMap(dataService::getVideoDescription);
    }

    @Provides @ScreenScope
    public DetailsOverviewRow provideDetailsOverviewRow(
            MediaBrowser.MediaItem mediaItem,
            DetailsActionsAdapter actionsAdapter) {
        //init row with initial info
        final DetailsOverviewRow row = new DetailsOverviewRow(VideoDescInfo.from(mediaItem));
        //add actions adapter
        row.setActionsAdapter(actionsAdapter);
        return row;
    }

    @Provides @ScreenScope
    public FullWidthDetailsOverviewRowPresenter provideDetailsOverviewPresenter(
            OnActionClickedListener actionClickedListener,
            DetailsVideoDescPresenter detailsVideoDescPresenter) {
        //create the row presenter
        final FullWidthDetailsOverviewRowPresenter detailsPresenter =
                new FullWidthDetailsOverviewRowPresenter(detailsVideoDescPresenter);
        //add actions click listener
        detailsPresenter.setOnActionClickedListener(actionClickedListener);
        return detailsPresenter;
    }

    @Provides @ScreenScope @Named("rowsAdapter")
    public ObjectAdapter provideRowsAdapter(
            DetailsOverviewRow detailsOverviewRow,
            FullWidthDetailsOverviewRowPresenter detailsPresenter,
            @Named("additionalRowsAdapter") ObjectAdapter rowsAdapter
    ) {
        //create selector for details row
        final ClassPresenterSelector defaultSelector = new ClassPresenterSelector();
        defaultSelector.addClassPresenter(DetailsOverviewRow.class, detailsPresenter);

        final ArrayList<Object> defaultRows = new ArrayList<>(2);
        defaultRows.add(detailsOverviewRow);

        PresenterSelector theirPresenterSelector = rowsAdapter.getPresenterSelector();
        if (theirPresenterSelector == null) {
            throw new IllegalArgumentException("Rows adapter must have a presenter selector");
        }

        //init wrapped selector for details row and their selector
        final PresenterSelectorWrapper selectorWrapper = new PresenterSelectorWrapper(
                defaultSelector, theirPresenterSelector);

        //init wrapped adapter to add default rows.
        return new ObjectAdapterWrapper(selectorWrapper, defaultRows, rowsAdapter);
    }

    @Provides @ScreenScope @Named("screenSubscriptions")
    public CompositeSubscription provideScreenSubscriptions() {
        return new CompositeSubscription();
    }

    public static class PresenterSelectorWrapper extends PresenterSelector {

        final PresenterSelector defaultSelector;
        final PresenterSelector wrappedSelector;

        public PresenterSelectorWrapper(
                PresenterSelector defaultSelector,
                PresenterSelector wrappedSelector
        ) {
            this.defaultSelector = defaultSelector;
            this.wrappedSelector = wrappedSelector;
        }

        @Override
        public Presenter getPresenter(Object item) {
            Presenter p = defaultSelector.getPresenter(item);
            if (p == null) {
                p = wrappedSelector.getPresenter(item);
            }
            return p;
        }

        @Override
        public Presenter[] getPresenters() {
            Presenter[] detailsPresenters = defaultSelector.getPresenters();
            if (detailsPresenters == null) detailsPresenters = new Presenter[0];
            Presenter[] wrappedPresenters = wrappedSelector.getPresenters();
            if (wrappedPresenters == null) wrappedPresenters = new Presenter[0];
            Presenter[] allPresenters = new Presenter[detailsPresenters.length + wrappedPresenters.length];
            System.arraycopy(detailsPresenters, 0, allPresenters, 0, detailsPresenters.length);
            System.arraycopy(wrappedPresenters, 0, allPresenters, detailsPresenters.length, wrappedPresenters.length);
            return allPresenters;
        }
    }

    public static class ObjectAdapterWrapper extends ObjectAdapter {

        final List<Object> injectedRows;
        final ObjectAdapter wrappedAdapter;

        public ObjectAdapterWrapper(
                PresenterSelector presenterSelector,
                List<Object> injectedRows,
                ObjectAdapter wrappedAdapter
        ) {
            super(presenterSelector);
            this.injectedRows = injectedRows;
            this.wrappedAdapter = wrappedAdapter;
            registerWrappedChanges();
        }

        @Override
        public int size() {
            return injectedRows.size() + wrappedAdapter.size();
        }

        @Override
        public Object get(int position) {
            if (position < injectedRows.size()) {
                return injectedRows.get(position);
            } else {
                return wrappedAdapter.get(position - injectedRows.size());
            }
        }

        void registerWrappedChanges(){
            wrappedAdapter.registerObserver(new DataObserver() {
                @Override
                public void onChanged() {
                    Timber.d("onChange");
                    notifyChanged();
                }

                @Override
                public void onItemRangeChanged(int positionStart, int itemCount) {
                    Timber.d("onChange");
                    notifyItemRangeChanged(injectedRows.size()+positionStart, itemCount);
                }

                @Override
                public void onItemRangeInserted(int positionStart, int itemCount) {
                    Timber.d("onChange");
                    notifyItemRangeInserted(injectedRows.size()+positionStart, itemCount);
                }

                @Override
                public void onItemRangeRemoved(int positionStart, int itemCount) {
                    Timber.d("onChange");
                    notifyItemRangeRemoved(injectedRows.size()+positionStart, itemCount);
                }
            });
        }
    }

}
