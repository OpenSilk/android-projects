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

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v17.leanback.app.PlaybackControlGlue;
import android.support.v17.leanback.app.PlaybackOverlayFragment;
import android.support.v17.leanback.widget.Action;
import android.support.v17.leanback.widget.PlaybackControlsRow;
import android.support.v17.leanback.widget.PlaybackControlsRowPresenter;
import android.support.v17.leanback.widget.PresenterSelector;
import android.support.v17.leanback.widget.SparseArrayObjectAdapter;

import org.opensilk.common.core.util.BundleHelper;
import org.opensilk.video.R;
import org.opensilk.video.playback.PlaybackService;
import org.opensilk.video.util.MediaMetadataHelper;
import org.opensilk.video.util.PlaybackStateHelper;
import org.videolan.libvlc.Media;

import java.util.List;

import timber.log.Timber;

/**
 * Created by drew on 3/22/16.
 */
public class PlaybackControlsGlue extends PlaybackControlGlue {

    private static final int[] FF_SPEEDS = new int[]{
            PLAYBACK_SPEED_FAST_L0,
            PLAYBACK_SPEED_FAST_L1,
            PLAYBACK_SPEED_FAST_L2,
            PLAYBACK_SPEED_FAST_L3,
            PLAYBACK_SPEED_FAST_L4,
    };
    private static final int SKIP_DELTA_MS = 60000 * 2;

    private final MediaController mMediaController;
    private final Handler mUpdateProgressHandler = new Handler();

    private Runnable mUpdateProgressRunnable;
    private int mSpeed = PLAYBACK_SPEED_NORMAL;
    private PlaybackState mState;
    private MediaMetadata mMetadata;

    public PlaybackControlsGlue(
            Context context,
            PlaybackOverlayFragment fragment,
            MediaController mediaController
    ) {
        super(context, fragment, FF_SPEEDS);
        mMediaController = mediaController;
    }

    MediaController getMediaController() {
        return mMediaController;
    }

    PlaybackState getPlaybackState() {
        return mState;
    }

    MediaMetadata getMetadata() {
        return mMetadata;
    }

    @Override
    public PlaybackControlsRowPresenter createControlsRowAndPresenter() {
        PlaybackControlsRowPresenter presenter = super.createControlsRowAndPresenter();
        presenter.setOnActionClickedListener(this);
//        presenter.setSecondaryActionsHidden(true);
        return presenter;
    }

    @Override
    protected SparseArrayObjectAdapter createPrimaryActionsAdapter(PresenterSelector presenterSelector) {
        SparseArrayObjectAdapter adapter = super.createPrimaryActionsAdapter(presenterSelector);
//        adapter.set(ACTION_CUSTOM_RIGHT_FIRST, new PlaybackControlsRow.MoreActions(getContext()));
        return adapter;
    }

    @Override
    public void onActionClicked(Action action) {
        if (action.getId() == R.id.cc_action) {
//            mMediaController.getTransportControls()
//                    .sendCustomAction(PlaybackService.ACTION.SET_SPU_TRACK, null);
        } else {
            super.onActionClicked(action);
        }
    }

    @Override
    public void enableProgressUpdating(boolean enable) {
        mUpdateProgressHandler.removeCallbacks(mUpdateProgressRunnable);
        if (enable) {
            if (mUpdateProgressRunnable == null) {
                mUpdateProgressRunnable = new Runnable() {
                    @Override
                    public void run() {
                        PlaybackControlsGlue.this.updateProgress();
                        mUpdateProgressHandler.postDelayed(this, getUpdatePeriod());
                    }
                };
            }
            mUpdateProgressHandler.post(mUpdateProgressRunnable);
        }
    }

    @Override
    public int getUpdatePeriod() {
        PlaybackState state = getPlaybackState();
        if (state != null ) {
            if (state.getPlaybackSpeed() > 1.0f) {
                return 100;
            }
        }
        return super.getUpdatePeriod();
    }

    @Override
    public boolean hasValidMedia() {
        return mMetadata != null;
    }

    @Override
    public boolean isMediaPlaying() {
        PlaybackState pbs = getPlaybackState();
        return pbs != null && PlaybackStateHelper.isPlaying(pbs);
    }

    @Override
    public CharSequence getMediaTitle() {
        MediaMetadata meta = getMetadata();
        return meta == null ? null : MediaMetadataHelper.getTitle(meta);
    }

    @Override
    public CharSequence getMediaSubtitle() {
        MediaMetadata meta = getMetadata();
        return meta == null ? null : MediaMetadataHelper.getSubTitle(meta);
    }

    @Override
    public int getMediaDuration() {
        MediaMetadata meta = getMetadata();
        return meta == null ? -1 : (int) MediaMetadataHelper.getDuration(meta);
    }

    @Override
    public Drawable getMediaArt() {
        return null;
//        MediaMetadata meta = getMetadata();
//        return meta == null ? null : new BitmapDrawable(getContext().getResources(),
//                MediaMetadataHelper.getIcon(meta));
    }

    @Override
    public long getSupportedActions() {
        long actions = ACTION_PLAY_PAUSE;
        actions |= ACTION_FAST_FORWARD | ACTION_REWIND;
        List<MediaSession.QueueItem> queue = mMediaController.getQueue();
        if (queue != null && !queue.isEmpty()) {
            actions |= ACTION_SKIP_TO_NEXT | ACTION_SKIP_TO_PREVIOUS;
        }
        return actions;
    }

    @Override
    public int getCurrentSpeedId() {
        return mSpeed;
    }

    @Override
    public int getCurrentPosition() {
        PlaybackState state = getPlaybackState();
        if (state != null) {
            long then = state.getLastPositionUpdateTime();
            float speedMultiplier = state.getPlaybackSpeed();
            long now = SystemClock.elapsedRealtime();
            return (int) ((state.getPosition() + (now - then) * speedMultiplier));
        }
        return 0;
    }

    @Override
    protected void startPlayback(int speed) {
        Timber.d("startPlayback(%d)", speed);
        if (mSpeed == speed) {
            Timber.w("no speed change ignoring...");
            return;
        }
        switch (speed) {
            case PLAYBACK_SPEED_NORMAL:
                mMediaController.getTransportControls().play();
                break;
            case PLAYBACK_SPEED_FAST_L0:
            case PLAYBACK_SPEED_FAST_L1:
            case PLAYBACK_SPEED_FAST_L2:
            case PLAYBACK_SPEED_FAST_L3:
            case PLAYBACK_SPEED_FAST_L4:
                mMediaController.getTransportControls().fastForward();
                break;
            case -PLAYBACK_SPEED_FAST_L0:
            case -PLAYBACK_SPEED_FAST_L1:
            case -PLAYBACK_SPEED_FAST_L2:
            case -PLAYBACK_SPEED_FAST_L3:
            case -PLAYBACK_SPEED_FAST_L4:
                mMediaController.getTransportControls().rewind();
                break;
            default:
                Timber.w("Unsupported speed level %d", speed);
                return;
        }
        mSpeed = speed;
    }

    @Override
    protected void pausePlayback() {
        Timber.d("pausePlayback()");
        mSpeed = PLAYBACK_SPEED_PAUSED;
        mMediaController.getTransportControls().pause();
    }

    @Override
    protected void skipToNext() {
        Timber.d("skipToNext()");
        mSpeed = PLAYBACK_SPEED_NORMAL;
        mMediaController.getTransportControls().skipToNext();
    }

    @Override
    protected void skipToPrevious() {
        Timber.d("skipToPrevious()");
        mSpeed = PLAYBACK_SPEED_NORMAL;
        mMediaController.getTransportControls().skipToPrevious();
    }

    //@Override
    protected void skipBackward() {
        mMediaController.getTransportControls().sendCustomAction(
                PlaybackService.ACTION.SEEK_DELTA,
                BundleHelper.b().putInt(-SKIP_DELTA_MS).get());
    }

    //@Override
    protected void skipForward() {
        mMediaController.getTransportControls().sendCustomAction(
                PlaybackService.ACTION.SEEK_DELTA,
                BundleHelper.b().putInt(SKIP_DELTA_MS).get());
    }

    @Override
    protected void onRowChanged(PlaybackControlsRow row) {
        Timber.d("onRowChanged()");
        PlaybackControlsFragment f = (PlaybackControlsFragment) getFragment();
        if (f != null) {
            f.updateRow(PlaybackControlsFragment.CONTROLS_ROW);
        }
    }

    public void onStateChanged(PlaybackState state) {
        mState = state;
        super.onStateChanged();
    }

    public void onMetadataChanged(MediaMetadata mediaMetadata) {
        mMetadata = mediaMetadata;
        super.onMetadataChanged();
    }

}
