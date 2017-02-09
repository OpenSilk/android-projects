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

package org.opensilk.video.tv.ui.folders;

import android.os.Bundle;
import android.support.v17.leanback.app.VerticalGridFragment;
import android.support.v17.leanback.widget.SearchOrbView;
import android.support.v17.leanback.widget.VerticalGridPresenter;
import android.view.View;
import android.widget.Toast;

import org.opensilk.common.dagger.DaggerService;
import org.opensilk.video.R;
import org.opensilk.video.data.ChildrenLoader;
import org.opensilk.video.data.ScannerService;
import org.opensilk.video.tv.ui.common.MediaItemClickListener;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;

import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.subscriptions.CompositeSubscription;
import timber.log.Timber;

/**
 * Created by drew on 3/22/16.
 */
public class FoldersScreenFragment extends VerticalGridFragment {

    FoldersScreenComponent mComponent;
    CompositeSubscription mSubscriptions;

    @Inject @Named("title") String mTitle;
    @Inject VerticalGridPresenter mGridPresenter;
    @Inject FoldersListAdapter mGridAdapter;
    @Inject ChildrenLoader mListLoader;
    @Inject @Named("isIndexed") Observable<Boolean> mIndexedObservable;

    boolean mIsIndexed = false;
    boolean mScanning = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FoldersActivityComponent activityComponent = DaggerService.getDaggerComponent(getActivity());
        mComponent = activityComponent.newScreenComponent();
        mSubscriptions = new CompositeSubscription();
        mComponent.inject(this);

        subscribeList();

        setTitle(mTitle);
        setGridPresenter(mGridPresenter);
        setAdapter(mGridAdapter);
        setOnItemViewClickedListener(new MediaItemClickListener());

        if (savedInstanceState == null) {
            prepareEntranceTransition();
        }
        // After 500ms, start the animation to transition the cards into view.
        Subscription s = rx.Observable.timer(500, TimeUnit.MILLISECONDS, AndroidSchedulers.mainThread())
                .subscribe((l) -> {
                    startEntranceTransition();
                });
        mSubscriptions.add(s);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mSubscriptions.unsubscribe();
        mSubscriptions = null;
        mComponent = null;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

//        getTitleView().setSearchAffordanceColors(new SearchOrbView.Colors(
//                getContext().getColor(R.color.search_opaque)));
//        getTitleView().setOnSearchClickedListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                v.playSoundEffect(SoundEffectConstants.CLICK);
//            }
//        });

        getTitleView().setAction0Drawable(
                getContext().getDrawable(R.drawable.arrow_left_48dp)
        );
        getTitleView().setAction0AffordanceColors(new SearchOrbView.Colors(
                getContext().getColor(R.color.search_opaque)
        ));
        getTitleView().setOnAction0ClickedListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //TODO not working
//                v.playSoundEffect(SoundEffectConstants.CLICK);
                getActivity().onNavigateUp();
            }
        });

        updateAction1Icon();
        getTitleView().setAction1AffordanceColors(new SearchOrbView.Colors(
                getContext().getColor(R.color.search_opaque)));
        getTitleView().setOnAction1ClickedListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                v.playSoundEffect(SoundEffectConstants.CLICK);
                if (mScanning) {
                    Toast.makeText(getContext(), "Scan already running", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (mIsIndexed) {
                    ScannerService.remove(getContext(), mComponent.mediaItem());
                    CharSequence title = mComponent.mediaItem().getDescription().getTitle();
                    Toast.makeText(getContext(), String.format(Locale.US, "Removing %s from index",
                            title), Toast.LENGTH_LONG).show();
                } else {
                    mScanning = true;
                    ScannerService.scan(getContext(), mComponent.mediaItem());
                    CharSequence title = mComponent.mediaItem().getDescription().getTitle();
                    Toast.makeText(getContext(), String.format(Locale.US, "Started scan on %s",
                            title), Toast.LENGTH_LONG).show();
                }
            }
        });

        getTitleView().setAction2Drawable(
                getContext().getDrawable(R.drawable.refresh_48dp)
        );
        getTitleView().setAction2AffordanceColors(new SearchOrbView.Colors(
                getContext().getColor(R.color.search_opaque)
        ));
        getTitleView().setOnAction2ClickedListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                v.playSoundEffect(SoundEffectConstants.CLICK);
                if (mScanning) {
                    Toast.makeText(getContext(), "Scan already running", Toast.LENGTH_SHORT).show();
                    return;
                }
                mScanning = true;
                ScannerService.scan(getContext(), mComponent.mediaItem());
                CharSequence title = mComponent.mediaItem().getDescription().getTitle();
                Toast.makeText(getContext(), String.format(Locale.US, "Started scan on %s",
                        title), Toast.LENGTH_LONG).show();
            }
        });
        updateAction2Visiblity();

        subscribeIndexedChanges();
    }

    private void updateAction1Icon() {
        int drawable = mIsIndexed ? R.drawable.minus_48dp : R.drawable.plus_48dp;
        if (getTitleView() != null && getContext() != null) {
            getTitleView().setAction1Drawable(getContext().getDrawable(drawable));
        }
    }

    private void updateAction2Visiblity() {
        int visibility = mIsIndexed ? View.VISIBLE : View.INVISIBLE;
        if (getTitleView() != null && getContext() != null) {
            getTitleView().getAction2View().setVisibility(visibility);
        }
    }

    private void subscribeList() {
        Subscription s = mListLoader.getListObservable()
                .subscribe(list -> {
                    mGridAdapter.swap(list);
                }, e -> {
                    Timber.w(e, "listItems");
                    //TODO show an actual error message
                    Toast.makeText(getContext(), "There was an error getting the items",
                            Toast.LENGTH_LONG).show();
                });
        mSubscriptions.add(s);
    }

    private void subscribeIndexedChanges(){
        Subscription s = mIndexedObservable
                .subscribe(indexed -> {
                    mIsIndexed = indexed;
                    mScanning = false;
                    updateAction1Icon();
                    updateAction2Visiblity();
                }, e -> {
                    Timber.w(e, "Subscribe isIndexed");
                });
        mSubscriptions.add(s);
    }

}
