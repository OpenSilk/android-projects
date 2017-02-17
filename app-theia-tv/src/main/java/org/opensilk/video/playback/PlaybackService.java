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

package org.opensilk.video.playback;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaDescription;
import android.media.MediaMetadata;
import android.media.Rating;
import android.media.browse.MediaBrowser;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Process;
import android.os.ResultReceiver;
import android.view.KeyEvent;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.FutureTarget;
import com.bumptech.glide.request.RequestOptions;

import org.apache.commons.lang3.StringUtils;
import org.opensilk.common.dagger.ActivityScope;
import org.opensilk.common.dagger.ForApplication;
import org.opensilk.common.util.BundleHelper;
import org.opensilk.video.VideoAppPreferences;
import org.opensilk.video.data.DataService;
import org.opensilk.video.data.MediaDescriptionUtil;
import org.opensilk.video.data.MediaMetaExtras;
import org.opensilk.video.data.VideosProviderClient;
import org.opensilk.video.tv.ui.details.DetailsActivity;
import org.opensilk.video.tv.ui.playback.PlaybackActivity;
import org.opensilk.video.util.PlaybackStateHelper;
import org.videolan.libvlc.IVLCVout;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;

import java.lang.reflect.Field;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.inject.Inject;

import timber.log.Timber;

import static android.media.session.PlaybackState.ACTION_PAUSE;
import static android.media.session.PlaybackState.ACTION_PLAY;
import static android.media.session.PlaybackState.ACTION_PLAY_FROM_URI;
import static android.media.session.PlaybackState.ACTION_STOP;
import static android.media.session.PlaybackState.STATE_BUFFERING;
import static android.media.session.PlaybackState.STATE_ERROR;
import static android.media.session.PlaybackState.STATE_FAST_FORWARDING;
import static android.media.session.PlaybackState.STATE_NONE;
import static android.media.session.PlaybackState.STATE_PAUSED;
import static android.media.session.PlaybackState.STATE_PLAYING;
import static android.media.session.PlaybackState.STATE_REWINDING;
import static android.media.session.PlaybackState.STATE_SKIPPING_TO_NEXT;
import static android.media.session.PlaybackState.STATE_SKIPPING_TO_PREVIOUS;
import static android.media.session.PlaybackState.STATE_SKIPPING_TO_QUEUE_ITEM;
import static android.media.session.PlaybackState.STATE_STOPPED;

/**
 * Created by drew on 3/21/16.
 */
@ActivityScope
public class PlaybackService {

    public interface ACTION {
        String SEEK_DELTA = "seek_delta";
        String SET_SPU_TRACK = "set_spu_track";
    }

    public interface CMD {
        String GET_SPU_TRACKS = "get_spu_tracks";
    }

    public static final int SEEK_DELTA_DURATION = 10000;

    private final Context mContext;
    private final VideoAppPreferences mSettings;
    private final VideosProviderClient mDbClient;
    private final VLCInstance mVLCInstance;
    private final PlaybackQueue mQueue;
    private final DataService mDataService;

    private final IVLCVout.Callback mVLCVOutCallback = new VLCVoutCallback();
    private final MediaPlayerEventListener mMediaPlayerEventListener = new MediaPlayerEventListener();
    private final MediaSessionCallback mMediaSessionCallback = new MediaSessionCallback();

    private MediaSession mMediaSession;
    private MediaPlayer mMediaPlayer;
    private HandlerThread mPlaybackThread;
    private Handler mPlaybackHandler;
    private Handler mMainHandler;

    private boolean mCreated;

    /* for getTime and seek */
    private long mForcedTime = -1;
    private long mLastTime = -1;

    private int mCurrentState = STATE_NONE;
    private float mPlaybackSpeed = 1.0f;
    private boolean mForceSeekDuringLoad;
    private long mSeekOnMedia = -1;
    private boolean mSeekable;
    private boolean mLoadingNext;
    private int mStateBeforeSeek = STATE_NONE;

    @Inject
    public PlaybackService(
            @ForApplication Context mContext,
            VideoAppPreferences mSettings,
            VideosProviderClient mDbClient,
            VLCInstance vlcInstance,
            PlaybackQueue queue,
            DataService mDataService
    ) {
        this.mContext = mContext;
        this.mSettings = mSettings;
        this.mDbClient = mDbClient;
        this.mVLCInstance = vlcInstance;
        this.mQueue = queue;
        this.mDataService = mDataService;
    }

    public MediaSession getMediaSession() {
        return mMediaSession;
    }

    private MediaPlayer newMediaPlayer() {
        final MediaPlayer mp = new MediaPlayer(mVLCInstance.get());
        mp.getVLCVout().addCallback(mVLCVOutCallback);
        return mp;
    }

    private MediaSession newMediaSession() {
        final MediaSession mediaSession = new MediaSession(mContext, "SilkVideoPlayer");
        mediaSession.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS|MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS);
        final ComponentName mediaButtonComponent = new ComponentName(mContext, MediaButtonReceiver.class);
        final PendingIntent mediaButtonIntent = PendingIntent.getBroadcast(mContext, 1,
                new Intent().setComponent(mediaButtonComponent), PendingIntent.FLAG_UPDATE_CURRENT);
        mediaSession.setMediaButtonReceiver(mediaButtonIntent);
        mediaSession.setSessionActivity(makeActivityIntent(null));
        return mediaSession;
    }

    public void onCreate() {
        mPlaybackThread = new HandlerThread("SilkPlayback", Process.THREAD_PRIORITY_MORE_FAVORABLE);
        mPlaybackThread.start();
        mPlaybackHandler = new Handler(mPlaybackThread.getLooper());
        mMainHandler = new Handler(Looper.getMainLooper());

        mMediaSession = newMediaSession();
        mMediaSession.setCallback(mMediaSessionCallback, mPlaybackHandler);

        mMediaPlayer = newMediaPlayer();
        mMediaPlayer.setEventListener(mMediaPlayerEventListener);
        mMediaPlayer.setEqualizer(VLCOptions.getEqualizer(mContext));

        mCreated = true;

        updateState(STATE_NONE);

    }

    public void onDestroy() {
        mCreated = false;

        mMediaSession.setActive(false);
        mMediaSession.release();
        mMediaSession = null;

        mMediaPlayer.setEventListener(null);
        mMediaPlayer.stop();
        mMediaPlayer.release();
        mMediaPlayer = null;

        mPlaybackHandler.removeCallbacksAndMessages(null);
        mPlaybackThread.quitSafely();

        mMainHandler.removeCallbacksAndMessages(null);

    }

    public void handleMediaKey(KeyEvent keyEvent) {
        mMediaSession.getController().dispatchMediaButtonEvent(keyEvent);
    }

    PendingIntent makeActivityIntent(MediaBrowser.MediaItem mediaItem) {
        final ComponentName activityComponent = new ComponentName(mContext, PlaybackActivity.class);
        final Intent intent = new Intent().setComponent(activityComponent).setAction(PlaybackActivity.ACTION_RESUME);
        if (mediaItem != null) {
            intent.putExtra(DetailsActivity.MEDIA_ITEM, mediaItem);
        }
        return PendingIntent.getActivity(mContext, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    void updateState(int state) {
        updateState(state, null);
    }

    void updateState(int state, String error) {
        long actions = ACTION_PLAY_FROM_URI;
        switch (state) {
            case STATE_PLAYING:
            case STATE_BUFFERING:
            case STATE_SKIPPING_TO_NEXT:
            case STATE_SKIPPING_TO_PREVIOUS:
            case STATE_SKIPPING_TO_QUEUE_ITEM:
            case STATE_FAST_FORWARDING:
            case STATE_REWINDING:
                actions |= ACTION_PAUSE | ACTION_STOP;
                break;
            case STATE_ERROR:
                break;
            case STATE_STOPPED:
            case STATE_PAUSED:
                actions |= ACTION_PLAY;
                break;
            case STATE_NONE:
                break;
        }
        PlaybackState.Builder builder = new PlaybackState.Builder()
                .setActions(actions)
                .setState(state, getTime(), mPlaybackSpeed);
        MediaSession.QueueItem currentItem = mQueue.getCurrent();
        if (currentItem != null) {
            builder.setActiveQueueItemId(currentItem.getQueueId());
        }
        if (state == STATE_ERROR) {
            if (error != null) {
                builder.setErrorMessage(error);
            } else {
                builder.setErrorMessage("Unknown error");
            }
        }
        mMediaSession.setPlaybackState(builder.build());
        mCurrentState = state;
        Timber.d("updateState %s pos=%d", PlaybackStateHelper.stringifyState(state), getTime());
    }

    void updateMetadata() {
        assertCreated();
        final Media media = mMediaPlayer.getMedia();
        final MediaBrowser.MediaItem mediaItem = mDbClient.getMedia(media.getUri());

        final MediaMetadata.Builder b = new MediaMetadata.Builder();
        CharSequence title;
        Uri artworkUri = null;
        long duration;
        if (mediaItem != null) {
            MediaDescription description = mediaItem.getDescription();
            title = description.getTitle();
            b.putText(MediaMetadata.METADATA_KEY_DISPLAY_TITLE, title);
            b.putText(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE, description.getSubtitle());
            if (description.getIconUri() != null) {
                b.putText(MediaMetadata.METADATA_KEY_DISPLAY_ICON_URI,
                        description.getIconUri().toString());
                artworkUri = description.getIconUri();
            }
            MediaMetaExtras metaExtras = MediaMetaExtras.from(description);
            b.putText(MediaMetadata.METADATA_KEY_TITLE, metaExtras.getMediaTitle());
            duration = metaExtras.getDuration();
        } else {
            title = media.getMeta(Media.Meta.Title);
            b.putText(MediaMetadata.METADATA_KEY_DISPLAY_TITLE, title);
            String artworkUrl = media.getMeta(Media.Meta.ArtworkURL);
            if (!StringUtils.isEmpty(artworkUrl)) {
                b.putText(MediaMetadata.METADATA_KEY_DISPLAY_ICON_URI, artworkUrl);
                artworkUri = Uri.parse(artworkUrl);
            }
            duration = mMediaPlayer.getLength();
        }
        b.putLong(MediaMetadata.METADATA_KEY_DURATION, duration);
        if (artworkUri != null) {
            RequestOptions options = new RequestOptions()
                    .fitCenter(mContext);
            FutureTarget<Bitmap> futureTarget = Glide.with(mContext)
                    .asBitmap()
                    .apply(options)
                    .load(artworkUri)
                    .submit();
            try {
                Bitmap bitmap = futureTarget.get(5000, TimeUnit.MILLISECONDS);
                b.putBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON, bitmap);
            } catch (InterruptedException|ExecutionException|TimeoutException e) {
                //pass
            }
        }
        mMediaSession.setMetadata(b.build());
        mMediaSession.setSessionActivity(makeActivityIntent(mediaItem));
    }

    private void assertCreated() {
        if (!mCreated) {
            throw new IllegalStateException("No mediaplayer! Must call onCreate()");
        }
    }

    public IVLCVout getVLCVout() {
        assertCreated();
        return mMediaPlayer.getVLCVout();
    }

    private long getTime() {
        assertCreated();
        if (true){
            return mMediaPlayer.getTime();
        }
        long time = mMediaPlayer.getTime();
        if (mForcedTime != -1 && mLastTime != -1) {
            /* XXX: After a seek, mService.getTime can return the position before or after
             * the seek position. Therefore we return mForcedTime in order to avoid the seekBar
             * to move between seek position and the actual position.
             * We have to wait for a valid position (that is after the seek position).
             * to re-init mLastTime and mForcedTime to -1 and return the actual position.
             */
            if (mLastTime > mForcedTime) {
                if (time <= mLastTime && time > mForcedTime || time > mLastTime)
                    mLastTime = mForcedTime = -1;
            } else {
                if (time > mForcedTime)
                    mLastTime = mForcedTime = -1;
            }
        }
        return mForcedTime == -1 ? time : mForcedTime;
    }

    private void seek(long position) {
        assertCreated();
        seek(position, mMediaPlayer.getLength());
    }

    private void seek(long position, float length) {
        assertCreated();
        if (!mForceSeekDuringLoad && mCurrentState == STATE_BUFFERING) {
            Timber.w("Ignoring seek while buffering");
            return;
        }
        mForceSeekDuringLoad = false;
        mStateBeforeSeek = mCurrentState;
        mCurrentState = STATE_BUFFERING;
        mForcedTime = position;
        mLastTime = mMediaPlayer.getTime();
        if (length == 0f) {
            mMediaPlayer.setTime(position);
        } else {
            mMediaPlayer.setPosition(position / length);
        }
    }

    private void seekDelta(int delta) {
        assertCreated();
        if (mMediaPlayer.getLength() <= 0 || !mMediaPlayer.isSeekable()) {
            return;        // unseekable stream
        }
        long position = getTime() + delta;
        if (position < 0) {
            position = 0;
        }
        seek(position);
    }

    private static float getSpeedMultiplier(float current) {
        if (current < 2.0f) {
            return 2.0f;
        } else if (current < 3.0f) {
            return 3.0f;
        } else if (current < 4.0f) {
            return 4.0f;
        } else if (current < 5.0f) {
            return 5.0f;
        } else if (current < 6.0f) {
            return 6.0f;
        } else {
            return 2.0f; //rollback round
        }
    }

    private void loadQueueItem(MediaSession.QueueItem queueItem) {
        Uri mediaUri = MediaDescriptionUtil.getMediaUri(queueItem.getDescription());
        final Media media = new Media(mVLCInstance.get(), mediaUri);
        int flags = 0;
        VLCOptions.setMediaOptions(media, mContext, flags);
        mMediaPlayer.setMedia(media);
        mMediaSession.setQueue(mQueue.getQueue());
    }

    private void updateCurrentItemLastPosition(long pos) {
        MediaSession.QueueItem queueItem = mQueue.getCurrent();
        if (queueItem == null) {
            return;
        }
        Uri mediaUri = MediaDescriptionUtil.getMediaUri(queueItem.getDescription());
        Timber.d("Update last_position=%d for %s", pos, mediaUri);
        mDbClient.updateMediaLastPosition(mediaUri, pos);
        mDataService.notifyChange(mediaUri);
    }

    private void updateCurrentItemDuration(long dur) {
        if (dur <= 0) {
            Timber.w("Invalid duration %d", dur);
            return;
        }
        MediaSession.QueueItem queueItem = mQueue.getCurrent();
        if (queueItem == null) {
            return;
        }
        Uri mediaUri = MediaDescriptionUtil.getMediaUri(queueItem.getDescription());
        Timber.d("Update duration=%d for %s", dur, mediaUri);
        mDbClient.updateMediaDuration(mediaUri, dur);
        mDataService.notifyChange(mediaUri);
    }

    class MediaSessionCallback extends MediaSession.Callback {
        public MediaSessionCallback() {
            super();
        }

        @Override
        public void onCommand(String command, Bundle args, ResultReceiver cb) {
            Timber.d("onCommand(%s)",command);
            switch (command) {
                case CMD.GET_SPU_TRACKS: {
                    MediaPlayer.TrackDescription[] tracks = mMediaPlayer.getSpuTracks();
                    if (tracks != null && tracks.length > 0) {
                        for (MediaPlayer.TrackDescription t : tracks) {
                            cb.send(1, BundleHelper.b().tag("spu_track")
                                    .putInt(t.id).putString(t.name).get());
                        }
                    } else {
                        cb.send(0, null);
                    }
                    break;
                }
            }
        }

        @Override
        public boolean onMediaButtonEvent(Intent mediaButtonIntent) {
            Timber.d("onMediaButtonEvent()");
            return super.onMediaButtonEvent(mediaButtonIntent);
        }

        @Override
        public void onPlay() {
            Timber.d("onPlay()");
            mMediaSession.setActive(true);
            if (mMediaPlayer.isPlaying()) {
                mMediaPlayer.setRate(1.0f);
                mPlaybackSpeed = 1.0f;
                updateState(STATE_PLAYING);
            } else {
                mMediaPlayer.play();
            }
//            updateState(STATE_PLAYING);
        }

        @Override
        public void onPlayFromMediaId(String mediaId, Bundle extras) {
            Timber.d("onPlayFromMediaId(%s)", mediaId);
        }

        @Override
        public void onPlayFromSearch(String query, Bundle extras) {
            Timber.d("onPlayFromSearch(%s)", query);
        }

        @Override
        public void onPlayFromUri(Uri uri, Bundle extras) {
            Timber.d("onPlayFromUri(%s)", uri);
            onPause();
            mQueue.loadFromUri(uri);
            MediaSession.QueueItem queueItem = mQueue.getCurrent();
            if (queueItem == null) {
                updateState(STATE_ERROR, "Failed to load queue");
                return;
            }
            mMediaSession.setQueueTitle(mQueue.getTitle());

            mSeekOnMedia = -1;
            boolean resume = extras.getBoolean("resume");
            if (resume) {
                MediaMetaExtras metaExtras = MediaMetaExtras.from(queueItem.getDescription());
                if (metaExtras.getLastPosition() > 0) {
                    mForceSeekDuringLoad = true;
                    mSeekOnMedia = metaExtras.getLastPosition();
                }
            }
            loadQueueItem(queueItem);
            onPlay();
        }

        @Override
        public void onSkipToQueueItem(long id) {
            Timber.d("onSkipToQueueItem(%d)", id);
            onPause();
            mQueue.moveToItem(id);
            MediaSession.QueueItem queueItem = mQueue.getCurrent();
            if (queueItem == null) {
                updateState(STATE_ERROR, "Failed to load queue");
                return;
            }
            loadQueueItem(queueItem);
            onPlay();
        }

        @Override
        public void onPause() {
            Timber.d("onPause()");
            updateCurrentItemLastPosition(getTime());
            mMediaPlayer.pause();
            updateState(STATE_PAUSED);
        }

        @Override
        public void onSkipToNext() {
            Timber.d("onSkipToNext()");
            onPause();
            MediaSession.QueueItem queueItem = mQueue.getNext();
            if (queueItem == null) {
                updateState(STATE_ERROR, "Unable to get next queue item");
                return;
            }
            updateState(STATE_SKIPPING_TO_NEXT);
            loadQueueItem(queueItem);
            onPlay();
        }

        @Override
        public void onSkipToPrevious() {
            Timber.d("onSkipToPrevious()");
            onPause();
            MediaSession.QueueItem queueItem = mQueue.getPrevious();
            if (queueItem == null) {
                updateState(STATE_ERROR, "Unable to get previous queue item");
                return;
            }
            updateState(STATE_SKIPPING_TO_NEXT);
            loadQueueItem(queueItem);
            onPlay();
        }

        @Override
        public void onFastForward() {
            Timber.d("onFastForward()");
            if (!mMediaPlayer.isPlaying()) {
                mMediaPlayer.play();
            }
            if (mPlaybackSpeed < 1.0f) {
                onPlay();
                return;
            }
            mPlaybackSpeed = getSpeedMultiplier(Math.abs(mPlaybackSpeed));
            Timber.d("onFastForward(%.02f)", mPlaybackSpeed);
            mMediaPlayer.setRate(mPlaybackSpeed);
            updateState(STATE_FAST_FORWARDING);
        }

        @Override
        public void onRewind() {
            Timber.d("onRewind()");
            if (!mMediaPlayer.isPlaying()) {
                mMediaPlayer.play();
            }
            if (mPlaybackSpeed != 1.0f) {
                onPlay();
                return;
            }
            //cant rewind so just skip
            onCustomAction(ACTION.SEEK_DELTA,
                    BundleHelper.b().putInt(-SEEK_DELTA_DURATION).get());
        }

        @Override
        public void onStop() {
            Timber.d("onStop()");
            updateCurrentItemLastPosition(getTime());
            mMediaPlayer.stop();
            updateState(STATE_STOPPED);
        }

        @Override
        public void onSeekTo(long pos) {
            Timber.d("onSeekTo(%d)", pos);
            seek(pos);
        }

        @Override
        public void onSetRating(Rating rating) {
            Timber.d("onSetRating(%s)", rating);
        }

        @Override
        public void onCustomAction(String action, Bundle extras) {
            Timber.d("onCustomAction(%s)", action);
            switch (action) {
                case ACTION.SEEK_DELTA: {
                    int delta = BundleHelper.getInt(extras);
                    seekDelta(delta);
                    break;
                }
                case ACTION.SET_SPU_TRACK: {
                    int track = BundleHelper.getInt(extras);
                    if (mMediaPlayer.getSpuTrack() != track) {
                        mMediaPlayer.setSpuTrack(track);
                    }
                    break;
                }
            }
        }
    }

    class MediaPlayerEventListener implements MediaPlayer.EventListener {
        @Override
        public void onEvent(final MediaPlayer.Event event) {
            mPlaybackHandler.post(new Runnable() {
                @Override
                public void run() {
                    switch (event.type) {
                        case MediaPlayer.Event.Opening:
                            Timber.i("MediaPlayer.Event.Opening");
                            updateState(STATE_BUFFERING);
                            break;
                        case MediaPlayer.Event.MediaChanged:
                            Timber.i("MediaPlayer.Event.MediaChanged");
                            mLoadingNext = false;
//                        updateMetadata();
                            break;
                        case MediaPlayer.Event.Playing:
                            Timber.i("MediaPlayer.Event.Playing");
                            if (mCurrentState != STATE_PLAYING) {
                                updateState(STATE_PLAYING);
                            }
//                        updateCurrentItemDuration(mMediaPlayer.getLength());
//                        updateMetadata();//to get duration
                            break;
                        case MediaPlayer.Event.Paused:
                            Timber.i("MediaPlayer.Event.Paused");
                            if (mCurrentState != STATE_PAUSED) {
                                updateState(STATE_PAUSED);
                            }
                            break;
                        case MediaPlayer.Event.Stopped:
                            Timber.i("MediaPlayer.Event.Stopped");
                            if (!mLoadingNext && mCurrentState != STATE_STOPPED) {
                                updateState(STATE_STOPPED);
                            }
                            break;
                        case MediaPlayer.Event.EndReached:
                            Timber.i("MediaPlayer.Event.EndReached");
                            updateCurrentItemLastPosition(mMediaPlayer.getLength());
                            MediaSession.QueueItem queueItem = mQueue.getNext();
                            if (queueItem != null) {
                                mLoadingNext = true;
                                loadQueueItem(queueItem);
                                mMediaSessionCallback.onPlay();
                            }
                            break;
                        case MediaPlayer.Event.EncounteredError:
                            Timber.i("MediaPlayer.Event.EncounteredError");
                            break;
                        case MediaPlayer.Event.TimeChanged:
//                        Timber.i("MediaPlayer.Event.TimeChanged time=%d", event.getTimeChanged());
                            if (mCurrentState == STATE_BUFFERING) {
                                Timber.i("MediaPlayer.Event.TimeChanged time=%d", event.getTimeChanged());
                                updateState(mStateBeforeSeek);
                            }
                            break;
                        case MediaPlayer.Event.PositionChanged:
//                    Timber.i("MediaPlayer.Event.PositionChanged pos=%.2f", event.getPositionChanged());
                            break;
                        case MediaPlayer.Event.Vout:
                            Timber.i("MediaPlayer.Event.Vout count=%d", event.getVoutCount());
                            break;
                        case MediaPlayer.Event.ESAdded:
                            Timber.i("MediaPlayer.Event.ESAdded");
                            break;
                        case MediaPlayer.Event.ESDeleted:
                            Timber.i("MediaPlayer.Event.ESDeleted");
                            break;
                        case MediaPlayer.Event.PausableChanged:
                            Timber.i("MediaPlayer.Event.PausableChanged pausable=%s", event.getPausable());
                            updateCurrentItemDuration(mMediaPlayer.getLength());
                            updateMetadata();//to get duration
                            break;
                        case MediaPlayer.Event.SeekableChanged:
                            Timber.i("MediaPlayer.Event.SeekableChanged seekable=%s", event.getSeekable());
                            if (event.getSeekable() && mSeekOnMedia > 0) {
                                mMediaSessionCallback.onSeekTo(mSeekOnMedia);
                            } else {
                                mForceSeekDuringLoad = false;
                            }
                            mSeekOnMedia = -1;
                            updateCurrentItemDuration(mMediaPlayer.getLength());
                            updateMetadata();//to get duration
                            break;
                        default:
                            try {
                                Field[] eventFields = MediaPlayer.Event.class.getDeclaredFields();
                                for (Field f : eventFields) {
                                    int val = f.getInt(null);
                                    if (val == event.type) {
                                        Timber.w("onEvent(%s)[Unhandled]", f.getName());
                                        return;
                                    }
                                }
                                Timber.e("onEvent(%d)[Unknown]", event.type);
                            } catch (Exception e) {
                                Timber.w(e, "onEvent");
                            }
                    }
                }
            });
        }
    }

    class VLCVoutCallback implements IVLCVout.Callback {
        @Override
        public void onNewLayout(IVLCVout ivlcVout, int width, int height, int visibleWidth,
                                int visibleHeight, int sarNum, int sarDen) {

        }

        @Override
        public void onSurfacesCreated(IVLCVout ivlcVout) {

        }

        @Override
        public void onSurfacesDestroyed(IVLCVout ivlcVout) {

        }

        @Override
        public void onHardwareAccelerationError(IVLCVout ivlcVout) {
            Timber.e("onHardwareAccelerationError()");
        }
    }
}
