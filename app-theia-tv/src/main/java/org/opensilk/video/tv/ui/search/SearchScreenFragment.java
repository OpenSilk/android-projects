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

package org.opensilk.video.tv.ui.search;

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.browse.MediaBrowser;
import android.os.Bundle;
import android.support.v17.leanback.app.SearchFragment;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.ListRowPresenter;
import android.support.v17.leanback.widget.ObjectAdapter;
import android.support.v17.leanback.widget.SpeechRecognitionCallback;

import org.opensilk.common.core.dagger2.DaggerFuncsKt;
import org.opensilk.video.data.MovieSearchLoader;
import org.opensilk.video.data.TvSeriesSearchLoader;
import org.opensilk.video.tv.ui.common.MediaItemClickListener;

import java.util.List;

import javax.inject.Inject;

import rx.Subscription;
import rx.functions.Action1;
import rx.subscriptions.CompositeSubscription;
import timber.log.Timber;

/**
 * Created by drew on 4/14/16.
 */
public class SearchScreenFragment extends SearchFragment implements SearchFragment.SearchResultProvider {

    private static final int REQUEST_SPEECH = 0x00000010;

    SearchScreenComponent mComponent;

    @Inject TvSeriesSearchLoader mTvSeriesSearchLoader;
    @Inject TvSeriesAdapter mTvSeriesAdapter;

    @Inject MovieSearchLoader mMovieSearchLoader;
    @Inject MoviesAdapter mMovieAdapter;

    ArrayObjectAdapter mAdapter;

    CompositeSubscription mSubscriptions;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SearchActivityComponent activityComponent = DaggerFuncsKt.getDaggerComponent(getActivity());
        SearchScreenModule module = new SearchScreenModule();
        mComponent = activityComponent.newSearchScreenComponent(module);
        mComponent.inject(this);

        mSubscriptions = new CompositeSubscription();
        subscribeSeriesLoader();
        subscribeMovieLoader();

        setupAdapter();
        setSearchResultProvider(this);
        setOnItemViewClickedListener(new MediaItemClickListener());

        if (!hasPermission(Manifest.permission.RECORD_AUDIO)) {
            Timber.w("Record audio permission revoked");
            // SpeechRecognitionCallback is not required and if not provided recognition will be
            // handled using internal speech recognizer, in which case you must have RECORD_AUDIO
            // permission
            setSpeechRecognitionCallback(new SpeechRecognitionCallback() {
                @Override
                public void recognizeSpeech() {
                    try {
                        startActivityForResult(getRecognizerIntent(), REQUEST_SPEECH);
                    } catch (ActivityNotFoundException e) {
                        Timber.e(e, "Cannot find activity for speech recognizer");
                    }
                }
            });
        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mSubscriptions.unsubscribe();
        mSubscriptions = null;
    }

    @Override
    public ObjectAdapter getResultsAdapter() {
        return mAdapter;
    }

    @Override
    public boolean onQueryTextChange(String newQuery) {
        mTvSeriesSearchLoader.requery(newQuery);
        mMovieSearchLoader.requery(newQuery);
        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        mTvSeriesSearchLoader.requery(query);
        mMovieSearchLoader.requery(query);
        return true;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_SPEECH:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        setSearchQuery(data, true);
                        break;
                    case Activity.RESULT_CANCELED:
                        // Once recognizer canceled, user expects the current activity to process
                        // the same BACK press as user doesn't know about overlay activity.
                        // However, you may not want this behaviour as it makes harder to
                        // fall back to keyboard input.
                        if (mTvSeriesAdapter.size() == 0
                                && mMovieAdapter.size() == 0) {
                            getActivity().onBackPressed();
                        }
                        break;
                    // the rest includes various recognizer errors, see {@link RecognizerIntent}
                }
                break;
        }
    }

    void setupAdapter() {
        mAdapter = new ArrayObjectAdapter(new ListRowPresenter());
        HeaderItem seriesHeader = new HeaderItem("TV Shows");
        mAdapter.add(new ListRow(seriesHeader, mTvSeriesAdapter));

        HeaderItem movieHeader = new HeaderItem("Movies");
        mAdapter.add(new ListRow(movieHeader, mMovieAdapter));
    }

    void subscribeSeriesLoader() {
        Subscription s = mTvSeriesSearchLoader.getListObservable()
                .subscribe(new Action1<List<MediaBrowser.MediaItem>>() {
                    @Override
                    public void call(List<MediaBrowser.MediaItem> list) {
                        mTvSeriesAdapter.swap(list);
                    }
                });
        mSubscriptions.add(s);
    }

    void subscribeMovieLoader() {
        Subscription s = mMovieSearchLoader.getListObservable()
                .subscribe(new Action1<List<MediaBrowser.MediaItem>>() {
                    @Override
                    public void call(List<MediaBrowser.MediaItem> list) {
                        mMovieAdapter.swap(list);
                    }
                });
        mSubscriptions.add(s);
    }

    private boolean hasPermission(final String permission) {
        final Context context = getActivity();
        return PackageManager.PERMISSION_GRANTED == context.getPackageManager().checkPermission(
                permission, context.getPackageName());
    }
}
