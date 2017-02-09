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

package org.opensilk.video.tv.ui.landing;

import android.os.Bundle;
import android.support.v17.leanback.app.BrowseFragment;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.ListRowPresenter;

import org.opensilk.common.core.dagger2.DaggerService;
import org.opensilk.video.R;
import org.opensilk.video.data.FoldersLoader;
import org.opensilk.video.data.MoviesLoader;
import org.opensilk.video.data.TvSeriesLoader;
import org.opensilk.video.tv.ui.common.MediaItemClickListener;
import org.opensilk.video.tv.ui.search.SearchActivity;

import java.util.Collections;

import javax.inject.Inject;

import rx.Subscription;
import rx.subscriptions.CompositeSubscription;
import timber.log.Timber;

/**
 * Created by drew on 3/15/16.
 */
public class LandingScreenFragment extends BrowseFragment {

    LandingScreenComponent mComponent;

    @Inject TvSeriesLoader mTvSeriesLoader;
    @Inject FoldersLoader mFoldersLoader;
    @Inject MoviesLoader mMoviesLoader;

    @Inject TvSeriesAdapter mTvSeriesAdapter;
    @Inject FavoritesAdapter mFavoritesAdapter;
    @Inject MoviesAdapter mMoviesAdapter;
    @Inject FoldersAdapter mFoldersAdapter;

    CompositeSubscription mSubscriptions;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LandingActivityComponent activityComponent = DaggerService.getDaggerComponent(getActivity());
        LandingScreenModule screenModule = new LandingScreenModule();
        mComponent = activityComponent.newLandingScreenComponent(screenModule);
        mSubscriptions = new CompositeSubscription();
        mComponent.inject(this);

        setupUIElements();
        loadRows();
        setOnItemViewClickedListener(new MediaItemClickListener());
        setOnSearchClickedListener(v -> {
            SearchActivity.start(getActivity());
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mSubscriptions.unsubscribe();
        mSubscriptions = null;
        mComponent = null;
    }

    private void loadRows() {

        ArrayObjectAdapter rowsAdapter = new ArrayObjectAdapter(new ListRowPresenter());

        HeaderItem favoritesHeader = new HeaderItem("Favorites");
        rowsAdapter.add(new ListRow(favoritesHeader, mFavoritesAdapter));

        HeaderItem tvShowsHeader = new HeaderItem("TV Shows");
        rowsAdapter.add(new ListRow(tvShowsHeader, mTvSeriesAdapter));
        Subscription seriesSubscription = mTvSeriesLoader.getListObservable()
                .subscribe(list -> {
                    Collections.shuffle(list);
                    mTvSeriesAdapter.swap(list);
                }, e -> {
                    Timber.w(e, "Failed loading tv series");
                });
        mSubscriptions.add(seriesSubscription);

        HeaderItem moviesHeader = new HeaderItem("Movies");
        rowsAdapter.add(new ListRow(moviesHeader, mMoviesAdapter));
        Subscription moviesSubscription = mMoviesLoader.getListObservable()
                .subscribe(list -> {
                    Collections.shuffle(list);
                    mMoviesAdapter.swap(list);
                }, e -> {
                    Timber.w(e, "movieSubscription");
                });
        mSubscriptions.add(moviesSubscription);

        HeaderItem foldersHeader = new HeaderItem("Folders");
        rowsAdapter.add(new ListRow(foldersHeader, mFoldersAdapter));
        Subscription foldersSubscription = mFoldersLoader.getListObservable()
                .subscribe(list -> {
                    mFoldersAdapter.swap(list);
                }, e -> {
                    Timber.w(e, "foldersSubscription");
                });
        mSubscriptions.add(foldersSubscription);

        HeaderItem settingsHeader = new HeaderItem("Preferences");
        rowsAdapter.add(new ListRow(settingsHeader, new ArrayObjectAdapter()));

        setAdapter(rowsAdapter);

    }

    private void setupUIElements() {
        // setBadgeDrawable(getActivity().getResources().getDrawable(
        // R.drawable.videos_by_google_banner));
        setTitle(getString(R.string.landing_title)); // Badge, when set, takes precedent
        // over title
        setHeadersState(HEADERS_ENABLED);
        setHeadersTransitionOnBackEnabled(true);

        // set fastLane (or headers) background color
        setBrandColor(getContext().getColor(R.color.fastlane_background));
        // set search icon color
        setSearchAffordanceColor(getContext().getColor(R.color.search_opaque));
    }


}
