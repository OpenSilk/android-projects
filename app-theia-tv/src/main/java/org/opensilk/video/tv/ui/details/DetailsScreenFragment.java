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

import android.graphics.drawable.Drawable;
import android.media.MediaDescription;
import android.net.Uri;
import android.os.Bundle;
import android.support.v17.leanback.app.BackgroundManager;
import android.support.v17.leanback.widget.DetailsOverviewRow;
import android.support.v17.leanback.widget.FullWidthDetailsOverviewSharedElementHelper;
import android.util.DisplayMetrics;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.request.transition.Transition;

import org.opensilk.common.core.dagger2.DaggerFuncsKt;
import org.opensilk.video.R;
import org.opensilk.video.data.MediaMetaExtras;
import org.opensilk.video.data.VideoDescInfo;

import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Scheduler;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Action1;
import timber.log.Timber;

/**
 * Created by drew on 3/15/16.
 */
public class DetailsScreenFragment extends android.support.v17.leanback.app.DetailsFragment {

    DetailsScreenComponent mComponent;
    boolean mBeforeTransition = true;
    long mTransitionStart;
    static final long transitionWindow = 2000;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final DetailsActivityComponent activityComponent = DaggerFuncsKt.getDaggerComponent(getContext());
        final MediaMetaExtras extras = MediaMetaExtras.from(
                activityComponent.mediaItem().getDescription());
        final DetailsScreenModule screenModule = new DetailsScreenModule(getContext());
        if (extras.isTvSeries()) {
            mComponent = activityComponent.newTvSeriesComponent(screenModule);
        } else if (extras.isTvEpisode()) {
            mComponent = activityComponent.newTvEpisodeComponent(screenModule);
        } else if (extras.isMovie()) {
            mComponent = activityComponent.newMovieComponent(screenModule);
        } else {
            mComponent = activityComponent.newVideoComponent(screenModule);
        }

        setupBackgroundImage();
        setupDetailsImage();
        subscribeDetailsChanges();

        mComponent.extender().onCreate(this);

        setAdapter(mComponent.rowsAdapter());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mComponent.extender().onDestroy(this);
        mComponent.subscriptions().unsubscribe();
        mComponent = null;
    }

    protected void setupBackgroundImage() {
        final BackgroundManager backgroundManager = BackgroundManager.getInstance(getActivity());
        backgroundManager.attach(getActivity().getWindow());
        final Drawable defaultBackground = getContext().getDrawable(R.drawable.default_background);
        backgroundManager.setDrawable(defaultBackground);
        final Subscription backgroundImageLoad = mComponent.backgroundImageUriObservable()
                .subscribe(new Action1<Uri>() {
                    @Override
                    public void call(Uri uri) {
                        Timber.d("Got new background uri %s", uri);
                        if (uri == null) {
                            backgroundManager.setDrawable(defaultBackground);
                            return;
                        }
                        final DisplayMetrics metrics = new DisplayMetrics();
                        getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);
                        final RequestOptions options = new RequestOptions()
                                .fitCenter(getContext())
                                .placeholder(defaultBackground);
                        final Target<Drawable> target = new SimpleTarget<Drawable>(metrics.widthPixels, metrics.heightPixels) {
                            @Override
                            public void onResourceReady(Drawable resource, Transition<? super Drawable> transition) {
                                backgroundManager.setDrawable(resource);
                            }
                        };
                        Glide.with(getContext())
                                .asDrawable()
                                .apply(options)
                                .load(uri)
                                .into(target);
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        backgroundManager.setDrawable(defaultBackground);
                    }
                });
        mComponent.subscriptions().add(backgroundImageLoad);
    }

    private void setupDetailsImage() {
        final MediaDescription mediaItem = mComponent.mediaDescription();
        final DetailsOverviewRow overviewRow = mComponent.detailsOverviewRow();
        final Drawable defaultIcon = getContext().getDrawable(R.drawable.movie_48dp);
        final FullWidthDetailsOverviewSharedElementHelper sharedElementHelper =
                new FullWidthDetailsOverviewSharedElementHelper();
        sharedElementHelper.setSharedElementEnterTransition(getActivity(),
                DetailsActivity.SHARED_ELEMENT_NAME);
        mComponent.detailsOverviewPresenter().setListener(sharedElementHelper);
        Subscription s = mComponent.iconImageUriObservable()
                .subscribe(new Action1<Uri>() {
                    @Override
                    public void call(final Uri uri) {
                        Timber.d("Got new icon uri %s", uri);
                        if (uri == null) {
                            return;
                        }
                        final int width = getResources().getDimensionPixelSize(R.dimen.detail_thumb_width);
                        final int height = getResources().getDimensionPixelSize(R.dimen.detail_thumb_height);
                        final RequestOptions options = new RequestOptions()
                                .centerInside(getContext())
                                .fallback(defaultIcon);
                        final Target<Drawable> target = new SimpleTarget<Drawable>(width, height) {
                            @Override
                            public void onResourceReady(Drawable resource, Transition<? super Drawable> transition) {
                                overviewRow.setImageDrawable(resource);
                            }
                        };
                        final Action0 loadAction = new Action0() {
                            @Override
                            public void call() {
                                Glide.with(getContext())
                                        .asDrawable()
                                        .apply(options)
                                        .load(uri)
                                        .into(target);
                            }
                        };
                        if (mBeforeTransition) {
                            mBeforeTransition = false;
                            mTransitionStart = System.currentTimeMillis();
                            loadAction.call();
                        } else {
                            //try not to mess up the entrance transition
                            long wait = mTransitionStart + transitionWindow - System.currentTimeMillis();
                            final Scheduler.Worker worker = AndroidSchedulers.mainThread().createWorker();
                            worker.schedule(new Action0() {
                                @Override
                                public void call() {
                                    loadAction.call();
                                    worker.unsubscribe();
                                }
                            }, wait > 0 ? wait : 0, TimeUnit.MILLISECONDS);
                        }
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        mBeforeTransition = false;
                        mTransitionStart = System.currentTimeMillis();
                        overviewRow.setImageDrawable(defaultIcon);
                    }
                });
        mComponent.subscriptions().add(s);
        //failsafe incase we don't get an icon uri
        Subscription transitionStart = Observable.timer(250, TimeUnit.MILLISECONDS, AndroidSchedulers.mainThread())
                .subscribe(new Action1<Long>() {
                    @Override
                    public void call(Long aLong) {
                        if (mBeforeTransition) {
                            mBeforeTransition = false;
                            mTransitionStart = System.currentTimeMillis();
                            Timber.d("Image took too long to load; continuing with default");
                            overviewRow.setImageDrawable(defaultIcon);
                        }
                    }
                });
        mComponent.subscriptions().add(transitionStart);
    }

    private void subscribeDetailsChanges() {
        Subscription s = mComponent.detailsOverviewChanges()
                .subscribe(new Action1<VideoDescInfo>() {
                    @Override
                    public void call(VideoDescInfo desc) {
                        mComponent.detailsOverviewRow().setItem(desc);
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable e) {
                        Timber.w(e, "detailsChanges");
                    }
                });
        mComponent.subscriptions().add(s);
    }

}
