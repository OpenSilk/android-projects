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

package org.opensilk.video.tv.ui.playback;

import android.app.Activity;
import android.content.Intent;
import android.media.MediaDescription;
import android.media.MediaMetadata;
import android.media.browse.MediaBrowser;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.support.annotation.NonNull;
import android.support.v17.leanback.app.PlaybackOverlayFragment;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.ClassPresenterSelector;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.ImageCardView;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.ListRowPresenter;
import android.support.v17.leanback.widget.OnItemViewClickedListener;
import android.support.v17.leanback.widget.PlaybackControlsRow;
import android.support.v17.leanback.widget.PlaybackControlsRowPresenter;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.RowPresenter;
import android.support.v17.leanback.widget.SparseArrayObjectAdapter;
import android.support.v17.leanback.widget.VerticalGridView;
import android.view.InputEvent;
import android.view.KeyEvent;
import android.view.MotionEvent;

import org.apache.commons.lang3.StringUtils;
import org.opensilk.common.core.dagger2.DaggerFuncsKt;
import org.opensilk.common.core.util.BundleHelper;
import org.opensilk.video.data.VideoDescInfo;
import org.opensilk.video.playback.PlaybackService;
import org.opensilk.video.tv.ui.details.DetailsActivity;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import rx.Observable;
import rx.Subscription;
import rx.functions.Action1;
import rx.subjects.Subject;
import rx.subscriptions.CompositeSubscription;
import timber.log.Timber;

/**
 * Created by drew on 3/20/16.
 */
public class PlaybackControlsFragment extends PlaybackOverlayFragment {

    final CompositeSubscription mSubscriptions = new CompositeSubscription();

    @Inject @Named("itemOverview") Observable<VideoDescInfo> mOverviewObservable;
    @Inject @Named("itemChangeSubject") Subject<MediaDescription, MediaDescription> mOverviewSubject;
    @Inject @Named("itemChange") Observable<MediaBrowser.MediaItem> mMediaItemObservable;

    final Handler mMainHandler = new Handler();

    PlaybackControlsComponent mComponent;
    MediaController mMediaController;
    SparseArrayObjectAdapter mAdapter;
    PlaybackControlsGlue mControlsRowGlue;
    ListRow mQueueRow;
    PlaybackOverviewRow mOverviewRow;
    ListRow mSubtitleRow;

    long mLastActiveItemId = MediaSession.QueueItem.UNKNOWN_ID;

    final static int CONTROLS_ROW = 1;
    final static int OVERVIEW_ROW = 2;
    final static int SUBTITLE_ROW = 3;
    final static int QUEUE_ROW = 4;

    final MediaControllerCallback mMediaControllerCallback = new MediaControllerCallback();
    final VerticalGridView.OnTouchInterceptListener mOnTouchInterceptListener =
            new VerticalGridView.OnTouchInterceptListener() {
                public boolean onInterceptTouchEvent(MotionEvent event) {
                    return onInterceptInputEvent(event);
                }
            };

    final VerticalGridView.OnKeyInterceptListener mOnKeyInterceptListener =
            new VerticalGridView.OnKeyInterceptListener() {
                public boolean onInterceptKeyEvent(KeyEvent event) {
                    return onInterceptInputEvent(event);
                }
            };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PlaybackActivityComponent activityComponent = DaggerFuncsKt.getDaggerComponent(getActivity());
        mComponent = activityComponent.newPlaybackControlsComponent(new PlaybackControlsModule(getActivity(), this));
        mComponent.inject(this);

        setBackgroundType(BG_NONE);

        //init mediaController
        mMediaController = getActivity().getMediaController();
        mMediaController.registerCallback(mMediaControllerCallback);

        //init primary adapter
        ClassPresenterSelector presenterSelector = new ClassPresenterSelector();
        mAdapter = new SparseArrayObjectAdapter(presenterSelector);

        //init controls row
        mControlsRowGlue = new PlaybackControlsGlue(getContext(), this, mMediaController);

        MediaMetadata mediaMetadata = mMediaController.getMetadata();
        if (mediaMetadata != null) {
            mControlsRowGlue.onMetadataChanged(mediaMetadata);
        }

        PlaybackState playbackState = mMediaController.getPlaybackState();
        if (playbackState != null) {
            mControlsRowGlue.onStateChanged(playbackState);
        }

        PlaybackControlsRowPresenter controlsRowPresenter = mControlsRowGlue.createControlsRowAndPresenter();
        PlaybackControlsRow controlsRow = mControlsRowGlue.getControlsRow();
//        SparseArrayObjectAdapter secondaryActionsAdapter =
//                new SparseArrayObjectAdapter(new ControlButtonPresenterSelector());
//        secondaryActionsAdapter.set(5, new PlaybackControlsRow.ClosedCaptioningAction(getContext()));
//        controlsRow.setSecondaryActionsAdapter(secondaryActionsAdapter);
        presenterSelector.addClassPresenter(PlaybackControlsRow.class, controlsRowPresenter);
        mAdapter.set(CONTROLS_ROW, controlsRow);

        //init overview row
        mOverviewRow = new PlaybackOverviewRow(new HeaderItem("Overview"));
        PlaybackOverviewRowPresenter overviewRowPresenter = new PlaybackOverviewRowPresenter();
        if (playbackState != null) {
            updateOverview(playbackState.getActiveQueueItemId());
        }
        presenterSelector.addClassPresenter(PlaybackOverviewRow.class, overviewRowPresenter);
        mAdapter.set(OVERVIEW_ROW, mOverviewRow);
        Subscription overviewSub = mOverviewObservable
                .subscribe(new Action1<VideoDescInfo>() {
                    @Override
                    public void call(VideoDescInfo info) {
                        mOverviewRow.setItem(info);
                    }
                });
        mSubscriptions.add(overviewSub);

        //init queue row
        QueueAdapter queueRowAdapter = new QueueAdapter();
        List<MediaSession.QueueItem> queue = mMediaController.getQueue();
        if (queue != null) {
            queueRowAdapter.swap(queue);
        }
        CharSequence queueTitle = mMediaController.getQueueTitle();
        if (StringUtils.isEmpty(queueTitle)) {
            queueTitle = "Queue";
        }
        mQueueRow = new ListRow(new HeaderItem(queueTitle.toString()), queueRowAdapter);
        presenterSelector.addClassPresenter(ListRow.class, new ListRowPresenter());
        mAdapter.set(QUEUE_ROW, mQueueRow);

        //init subtitle row
        SubtitleAdapter subtitleAdapter = new SubtitleAdapter();
        mSubtitleRow = new ListRow(new HeaderItem("Subtitles"), subtitleAdapter);
        //not adding yet

        //set adapter
        setAdapter(mAdapter);
        setOnItemViewClickedListener(new ItemClickListener());

        //on new items update the activity result
        Subscription resultSub = mMediaItemObservable.subscribe(new Action1<MediaBrowser.MediaItem>() {
            @Override
            public void call(MediaBrowser.MediaItem item) {
                Activity activity = getActivity();
                if (activity != null) {
                    Intent result = new Intent().putExtra(DetailsActivity.MEDIA_ITEM, item);
                    activity.setResult(Activity.RESULT_FIRST_USER + 12, result);
                }
            }
        });
        mSubscriptions.add(resultSub);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mSubscriptions.clear();
        mMediaController.unregisterCallback(mMediaControllerCallback);
        mMediaController = null;
        mAdapter = null;
        mControlsRowGlue.enableProgressUpdating(false);
        mControlsRowGlue = null;
        mOverviewObservable = null;
        mOverviewSubject = null;
        mComponent = null;
    }

    @Override
    public void onResume() {
        super.onResume();
        //replace supers listeners with ours
        getVerticalGridView().setOnKeyInterceptListener(mOnKeyInterceptListener);
        getVerticalGridView().setOnTouchInterceptListener(mOnTouchInterceptListener);
    }

    @Override
    protected boolean onInterceptInputEvent(InputEvent event) {
        boolean hidden = areControlsHidden();
        Timber.d("onInterceptInputEvent(%s) hidden = %s", event, hidden);
        boolean consumeEvent = false;
        boolean handled = false;
        int keyCode = KeyEvent.KEYCODE_UNKNOWN;
        if (event instanceof KeyEvent) {
            keyCode = ((KeyEvent) event).getKeyCode();
        }
        switch (keyCode) {
//            case KeyEvent.KEYCODE_DPAD_CENTER:
//            case KeyEvent.KEYCODE_DPAD_DOWN:
//            case KeyEvent.KEYCODE_DPAD_UP:
            case KeyEvent.KEYCODE_DPAD_LEFT:
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                if (hidden) {
                    if (((KeyEvent) event).getAction() == KeyEvent.ACTION_DOWN) {
                        int delta = keyCode == KeyEvent.KEYCODE_DPAD_LEFT ? -10000 : 15000;
                        consumeEvent = seekDelta(delta);
                    }
                    handled = true;
                }
                break;
        }
        return handled ? consumeEvent : super.onInterceptInputEvent(event);
    }

    private boolean seekDelta(int delta){
        if (mMediaController != null) {
            mMediaController.getTransportControls().sendCustomAction(
                    PlaybackService.ACTION.SEEK_DELTA,
                    BundleHelper.b().putInt(delta).get());
            return true;
        }
        return false;
    }

    public void updateRow(int row) {
        if (mAdapter != null) {
            mAdapter.notifyArrayItemRangeChanged(mAdapter.indexOf(row), 1);
        }
    }

    void updateOverview(long itemId) {
        if (mMediaController == null || itemId == mLastActiveItemId) {
            return;
        }
        List<MediaSession.QueueItem> q = mMediaController.getQueue();
        if (q == null) {
            return;
        }
        for (MediaSession.QueueItem i : q) {
            if (i.getQueueId() == itemId) {
                MediaDescription d = i.getDescription();
                mOverviewSubject.onNext(d);
                break;
            }
        }
    }

    void updateSubtitles() {
        if (mMediaController == null || mAdapter == null || mSubtitleRow == null) {
            return;
        }
        mAdapter.clear(SUBTITLE_ROW);
        final SubtitleAdapter subtitleAdapter = (SubtitleAdapter) mSubtitleRow.getAdapter();
        subtitleAdapter.clear();
        mMediaController.sendCommand(PlaybackService.CMD.GET_SPU_TRACKS, null, new ResultReceiver(mMainHandler) {
            @Override
            protected void onReceiveResult(int resultCode, Bundle resultData) {
                switch (resultCode) {
                    case 0: break;
                    case 1: {
                        if (mAdapter.indexOf(SUBTITLE_ROW) < 0) {
                            mAdapter.set(SUBTITLE_ROW, mSubtitleRow);
                        }
                        subtitleAdapter.add(resultData);
                        break;
                    }
                }
            }
        });
    }

    class MediaControllerCallback extends MediaController.Callback {

        @Override
        public void onPlaybackStateChanged(@NonNull PlaybackState state) {
            Timber.d("onPlaybackStateChanged(%s)", state);
            int nextState = state.getState();
            if (nextState == PlaybackState.STATE_STOPPED) {
                getActivity().finish();
                return;
            }
            if (mControlsRowGlue != null) {
                mControlsRowGlue.onStateChanged(state);
            }
            if (mOverviewRow != null) {
                updateOverview(state.getActiveQueueItemId());
            }
            if (nextState == PlaybackState.STATE_PLAYING) {
                updateSubtitles();
            }
        }

        @Override
        public void onMetadataChanged(MediaMetadata metadata) {
            Timber.d("onMetadataChanged(%s)", metadata);
            if (mControlsRowGlue != null) {
                mControlsRowGlue.onMetadataChanged(metadata);
            }
        }

        @Override
        public void onQueueChanged(List<MediaSession.QueueItem> queue) {
            Timber.d("onQueueChanged(%s)", queue);
            if (mQueueRow != null) {
                ArrayObjectAdapter adapter = (ArrayObjectAdapter) mQueueRow.getAdapter();
                if (queue == null || queue.isEmpty()) {
                    adapter.clear();
                } else {
                    adapter.swap(queue);
                }
            }
        }

        @Override
        public void onQueueTitleChanged(CharSequence title) {
            Timber.d("onQueueTitleChanged(%s)", title);
            if (mQueueRow != null) {
                mQueueRow.setHeaderItem(new HeaderItem(title.toString()));
                updateRow(QUEUE_ROW);
            }
        }
    }

    class ItemClickListener implements OnItemViewClickedListener {
        @Override
        public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item, RowPresenter.ViewHolder rowViewHolder, Row row) {
            if ((item instanceof MediaSession.QueueItem)) {
                MediaSession.QueueItem queueItem = (MediaSession.QueueItem) item;
                if (mMediaController != null) {
                    mMediaController.getTransportControls().skipToQueueItem(queueItem.getQueueId());
                }
            } else if (item instanceof Bundle) {
                Bundle bundle = (Bundle) item;
                if ("spu_track".equals(BundleHelper.getTag(bundle))) {
                    if (mMediaController != null) {
                        mMediaController.getTransportControls().sendCustomAction(
                                PlaybackService.ACTION.SET_SPU_TRACK, bundle);
                    }
                }
            }
        }
    }
}
