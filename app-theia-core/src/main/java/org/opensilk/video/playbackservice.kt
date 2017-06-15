package org.opensilk.video

import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.Rating
import android.media.browse.MediaBrowser
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.media.session.PlaybackState.*
import android.net.Uri
import android.os.*
import android.service.media.MediaBrowserService
import org.opensilk.common.util.BundleHelper
import org.opensilk.media.playback.PlaybackQueue
import org.videolan.libvlc.IVLCVout
import org.videolan.libvlc.MediaPlayer
import timber.log.Timber
import kotlin.properties.Delegates

/**
 * Created by drew on 6/8/17.
 */

class VideoPlaybackService: MediaBrowserService() {
    override fun onLoadChildren(parentId: String?, result: Result<MutableList<MediaBrowser.MediaItem>>) {

    }

    override fun onGetRoot(clientPackageName: String?, clientUid: Int, rootHints: Bundle?): BrowserRoot {
        return BrowserRoot("0", null)
    }
}

class PlaybackSession: MediaSession.Callback(), MediaPlayer.EventListener {

    interface ACTION {
        companion object {
            val SEEK_DELTA = "seek_delta"
            val SET_SPU_TRACK = "set_spu_track"
        }
    }

    interface CMD {
        companion object {
            val GET_SPU_TRACKS = "get_spu_tracks"
        }
    }

    val SEEK_DELTA_DURATION = 10000

    private val mContext: Context
    private val mSettings: VideoAppPreferences
    private val mDbClient: VideosProviderClient
    private val mVLCInstance: VLCInstance
    private val mQueue: PlaybackQueue
    private val mDataService: DataService

    private val mVLCVOutCallback = VLCVoutCallback()
    private val mMediaPlayerEventListener = MediaPlayerEventListener()
    private val mMediaSessionCallback = MediaSessionCallback()

    private var mMediaSession: MediaSession = null
    private var mMediaPlayer: MediaPlayer = null
    private var mPlaybackThread: HandlerThread = null
    private var mPlaybackHandler: Handler = null
    private var mMainHandler: Handler = null

    private var mCreated: Boolean = false

    /* for getTime and seek */
    private var mForcedTime: Long = -1
    private var mLastTime: Long = -1

    private var mCurrentState = STATE_NONE
    private var mPlaybackSpeed = 1.0f
    private var mForceSeekDuringLoad: Boolean = false
    private var mSeekOnMedia: Long = -1
    private val mSeekable: Boolean = false
    private var mLoadingNext: Boolean = false
    private var mStateBeforeSeek = STATE_NONE

    /*
     * Start mediasession callback methods
     */

    override fun onCommand(command: String, args: Bundle?, cb: ResultReceiver?) {
        Timber.d("onCommand(%s)", command)
        when (command) {
            CMD.GET_SPU_TRACKS -> {
                val tracks = mMediaPlayer.spuTracks
                if (tracks != null && tracks.isNotEmpty()) {
                    for (t in tracks) {
                        cb!!.send(1, BundleHelper.b().tag("spu_track").putInt(t.id).putString(t.name).get())
                    }
                } else {
                    cb!!.send(0, null)
                }
            }
        }
    }

    override fun onMediaButtonEvent(mediaButtonIntent: Intent): Boolean {
        Timber.d("onMediaButtonEvent()")
        return super.onMediaButtonEvent(mediaButtonIntent)
    }

    override fun onPlay() {
        Timber.d("onPlay()")
        mMediaSession.setActive(true)
        if (mMediaPlayer.isPlaying()) {
            mMediaPlayer.setRate(1.0f)
            mPlaybackSpeed = 1.0f
            updateState(STATE_PLAYING)
        } else {
            mMediaPlayer.play()
        }
        //            updateState(STATE_PLAYING);
    }

    override fun onPlayFromMediaId(mediaId: String, extras: Bundle) {
        Timber.d("onPlayFromMediaId(%s)", mediaId)
    }

    override fun onPlayFromSearch(query: String, extras: Bundle) {
        Timber.d("onPlayFromSearch(%s)", query)
    }

    override fun onPlayFromUri(uri: Uri, extras: Bundle) {
        Timber.d("onPlayFromUri(%s)", uri)
        onPause()
        mQueue.loadFromUri(uri)
        val queueItem = mQueue.getCurrent()
        if (queueItem == null) {
            updateState(STATE_ERROR, "Failed to load queue")
            return
        }
        mMediaSession.setQueueTitle(mQueue.getTitle())

        mSeekOnMedia = -1
        val resume = extras.getBoolean("resume")
        if (resume) {
            val metaExtras = MediaMetaExtras.from(queueItem!!.getDescription())
            if (metaExtras.getLastPosition() > 0) {
                mForceSeekDuringLoad = true
                mSeekOnMedia = metaExtras.getLastPosition()
            }
        }
        loadQueueItem(queueItem)
        onPlay()
    }

    override fun onSkipToQueueItem(id: Long) {
        Timber.d("onSkipToQueueItem(%d)", id)
        onPause()
        mQueue.moveToItem(id)
        val queueItem = mQueue.getCurrent()
        if (queueItem == null) {
            updateState(STATE_ERROR, "Failed to load queue")
            return
        }
        loadQueueItem(queueItem)
        onPlay()
    }

    override fun onPause() {
        Timber.d("onPause()")
        updateCurrentItemLastPosition(getTime())
        mMediaPlayer.pause()
        updateState(STATE_PAUSED)
    }

    override fun onSkipToNext() {
        Timber.d("onSkipToNext()")
        onPause()
        val queueItem = mQueue.getNext()
        if (queueItem == null) {
            updateState(STATE_ERROR, "Unable to get next queue item")
            return
        }
        updateState(STATE_SKIPPING_TO_NEXT)
        loadQueueItem(queueItem)
        onPlay()
    }

    override fun onSkipToPrevious() {
        Timber.d("onSkipToPrevious()")
        onPause()
        val queueItem = mQueue.getPrevious()
        if (queueItem == null) {
            updateState(STATE_ERROR, "Unable to get previous queue item")
            return
        }
        updateState(STATE_SKIPPING_TO_NEXT)
        loadQueueItem(queueItem)
        onPlay()
    }

    override fun onFastForward() {
        Timber.d("onFastForward()")
        if (!mMediaPlayer.isPlaying()) {
            mMediaPlayer.play()
        }
        if (mPlaybackSpeed < 1.0f) {
            onPlay()
            return
        }
        mPlaybackSpeed = getSpeedMultiplier(Math.abs(mPlaybackSpeed))
        Timber.d("onFastForward(%.02f)", mPlaybackSpeed)
        mMediaPlayer.setRate(mPlaybackSpeed)
        updateState(STATE_FAST_FORWARDING)
    }

    override fun onRewind() {
        Timber.d("onRewind()")
        if (!mMediaPlayer.isPlaying()) {
            mMediaPlayer.play()
        }
        if (mPlaybackSpeed != 1.0f) {
            onPlay()
            return
        }
        //cant rewind so just skip
        onCustomAction(ACTION.SEEK_DELTA,
                BundleHelper.b().putInt(-SEEK_DELTA_DURATION).get())
    }

    override fun onStop() {
        Timber.d("onStop()")
        updateCurrentItemLastPosition(getTime())
        mMediaPlayer.stop()
        updateState(STATE_STOPPED)
    }

    override fun onSeekTo(pos: Long) {
        Timber.d("onSeekTo(%d)", pos)
        seek(pos)
    }

    override fun onSetRating(rating: Rating) {
        Timber.d("onSetRating(%s)", rating)
    }

    override fun onCustomAction(action: String, extras: Bundle?) {
        Timber.d("onCustomAction(%s)", action)
        when (action) {
            ACTION.SEEK_DELTA -> {
                val delta = BundleHelper.getInt(extras)
                seekDelta(delta)
            }
            ACTION.SET_SPU_TRACK -> {
                val track = BundleHelper.getInt(extras)
                if (mMediaPlayer.getSpuTrack() != track) {
                    mMediaPlayer.setSpuTrack(track)
                }
            }
        }
    }

    /*
     * End mediasession callback methods
     */

    fun newPlaybackStateBuilder(state: Int): PlaybackState.Builder {
        var actions = ACTION_PLAY_FROM_MEDIA_ID or when (state) {

            STATE_PLAYING -> ACTION_PAUSE or ACTION_SEEK_TO

            STATE_BUFFERING -> ACTION_PAUSE or ACTION_SEEK_TO

            STATE_SKIPPING_TO_NEXT,
            STATE_SKIPPING_TO_PREVIOUS,
            STATE_SKIPPING_TO_QUEUE_ITEM -> ACTION_PAUSE

            STATE_FAST_FORWARDING,
            STATE_REWINDING -> ACTION_PLAY or ACTION_PAUSE

            STATE_PAUSED -> ACTION_PLAY

            STATE_ERROR,
            STATE_STOPPED,
            STATE_NONE -> 0
            else -> 0
        }
        if ((actions and ACTION_PAUSE) == ACTION_PAUSE) {
            if (mMediaPlayer.isSeekable) {
                actions = actions or ACTION_SEEK_TO
            }
        }
        val builder = PlaybackState.Builder()
                .setActions(actions)
                .setState(state, mMediaPlayer.time, mPlaybackSpeed)
        val currentItem = mQueue.getCurrent()
        if (currentItem != null) {
            builder.setActiveQueueItemId(currentItem.getQueueId())
        }
        return builder
    }

    /*
     * Start event listener methods
     */

    var mPlaybackState: PlaybackState by Delegates.observable(PlaybackState.Builder().build(), { _,_,nv ->
        mMediaSession.setPlaybackState(nv)
    })

    fun onEventOpening(event: MediaPlayer.Event) {
        mPlaybackState = newPlaybackStateBuilder().setState(STATE_BUFFERING,
                mMediaPlayer.time, mPlaybackSpeed).build()
    }

    fun onEventMediaChanged(event: MediaPlayer.Event) {
        mLoadingNext = false
    }

    fun onEventPlaying(event: MediaPlayer.Event) {
        if (mPlaybackState.state != STATE_PLAYING) {
            mPlaybackState = newPlaybackStateBuilder(STATE_PLAYING)
                    .setState(STATE_PLAYING, mMediaPlayer.time, mPlaybackSpeed)
                    .setActions(ACTION_PLAY_FROM_MEDIA_ID or ACTION_PAUSE
                            or ACTION_SKIP_TO_NEXT or ACTION_SKIP_TO_PREVIOUS).build()
        }
    }

    fun onEventPaused(event: MediaPlayer.Event) {
        if (mCurrentState != STATE_PAUSED) {
            mPlaybackState = newPlaybackStateBuilder().setState(STATE_PAUSED,
                    mMediaPlayer.time, mPlaybackSpeed).build()
        }
    }

    fun onEventStopped(event: MediaPlayer.Event) {
        if (!mLoadingNext && mCurrentState != STATE_STOPPED) {
                        updateState(STATE_STOPPED)
                    }
    }

    fun onEventEndReached(event: MediaPlayer.Event) {
        updateCurrentItemLastPosition(mMediaPlayer.getLength())
                    val queueItem = mQueue.getNext()
                    if (queueItem != null) {
                        mLoadingNext = true
                        loadQueueItem(queueItem)
                        mMediaSessionCallback.onPlay()
                    }
    }

    fun onEventTimeChanged(event: MediaPlayer.Event) {
        if (mCurrentState == STATE_BUFFERING) {
                        Timber.i("MediaPlayer.Event.TimeChanged time=%d", event.timeChanged)
                        updateState(mStateBeforeSeek)
                    }
    }

    fun onEventPausableChanged(event: MediaPlayer.Event) {
        updateCurrentItemDuration(mMediaPlayer.getLength())
                    updateMetadata()//to get duration
    }

    fun onEventSeekableChanged(event: MediaPlayer.Event) {
        if (event.seekable && mSeekOnMedia > 0) {
                        mMediaSessionCallback.onSeekTo(mSeekOnMedia)
                    } else {
                        mForceSeekDuringLoad = false
                    }
                    mSeekOnMedia = -1
                    updateCurrentItemDuration(mMediaPlayer.getLength())
                    updateMetadata()//to get duration
    }


    override fun onEvent(event: MediaPlayer.Event) {
        mPlaybackHandler.post(Runnable {
            when (event.type) {
                MediaPlayer.Event.Opening -> {
                    Timber.i("MediaPlayer.Event.Opening")
                    onEventMediaChanged(event)
                }
                MediaPlayer.Event.MediaChanged -> {
                    Timber.i("MediaPlayer.Event.MediaChanged")
                    onEventMediaChanged(event)
                }
                MediaPlayer.Event.Playing -> {
                    Timber.i("MediaPlayer.Event.Playing")
                    onEventPlaying(event)
                }
                MediaPlayer.Event.Paused -> {
                    Timber.i("MediaPlayer.Event.Paused")
                    onEventPaused(event)
                }
                MediaPlayer.Event.Stopped -> {
                    Timber.i("MediaPlayer.Event.Stopped")
                    onEventStopped(event)
                }
                MediaPlayer.Event.EndReached -> {
                    Timber.i("MediaPlayer.Event.EndReached")
                    onEventEndReached(event)
                }
                MediaPlayer.Event.EncounteredError -> {
                    Timber.i("MediaPlayer.Event.EncounteredError")
                }
                MediaPlayer.Event.TimeChanged -> {
                    onEventTimeChanged(event)
                }
                MediaPlayer.Event.PositionChanged -> {
                }
                MediaPlayer.Event.Vout -> {
                    Timber.i("MediaPlayer.Event.Vout count=%d", event.voutCount)
                }
                MediaPlayer.Event.ESAdded -> {
                    Timber.i("MediaPlayer.Event.ESAdded")
                }
                MediaPlayer.Event.ESDeleted -> {
                    Timber.i("MediaPlayer.Event.ESDeleted")
                }
                MediaPlayer.Event.PausableChanged -> {
                    Timber.i("MediaPlayer.Event.PausableChanged pausable=%s", event.pausable)
                    onEventPausableChanged(event)
                }
                MediaPlayer.Event.SeekableChanged -> {
                    Timber.i("MediaPlayer.Event.SeekableChanged seekable=%s", event.seekable)
                    onEventSeekableChanged(event)
                }
                else -> try {
                    val eventFields = MediaPlayer.Event::class.java.declaredFields
                    for (f in eventFields) {
                        val type = f.getInt(null)
                        if (type == event.type) {
                            Timber.w("onEvent(%s)[Unhandled]", f.name)
                            return@Runnable
                        }
                    }
                    Timber.e("onEvent(%d)[Unknown]", event.type)
                } catch (e: Exception) {
                    Timber.w(e, "onEvent")
                }
            }
        })
    }

    internal inner class VLCVoutCallback : IVLCVout.Callback {
        override fun onNewLayout(ivlcVout: IVLCVout, width: Int, height: Int, visibleWidth: Int,
                                 visibleHeight: Int, sarNum: Int, sarDen: Int) {

        }

        override fun onSurfacesCreated(ivlcVout: IVLCVout) {

        }

        override fun onSurfacesDestroyed(ivlcVout: IVLCVout) {

        }

        override fun onHardwareAccelerationError(ivlcVout: IVLCVout) {
            Timber.e("onHardwareAccelerationError()")
        }
    }

}

fun PlaybackState._newBuilder(): PlaybackState.Builder {
    val bob = PlaybackState.Builder()
            .setState(state, position, playbackSpeed, lastPositionUpdateTime)
            .setActions(actions)
            .setActiveQueueItemId(activeQueueItemId)
            .setBufferedPosition(bufferedPosition)
            .setErrorMessage(errorMessage)
            .setExtras(extras)
    for (action in customActions) {
        bob.addCustomAction(action)
    }
    return bob
}

