package org.opensilk.video

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
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import org.opensilk.common.rx.subscribeIgnoreError
import org.opensilk.media.*
import org.opensilk.media.playback.ExoPlayerRenderer
import org.opensilk.media.playback.PlaybackQueue
import timber.log.Timber
import javax.inject.Inject
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

class PlaybackExtras {
    private val bundle: Bundle
    public constructor(): this(Bundle())
    internal constructor(bundle: Bundle) {
        this.bundle = bundle
    }

    var resume: Boolean
        set(value) = bundle.putBoolean("resume", value)
        get() = bundle.getBoolean("resume", false)

    fun bundle() : Bundle {
        return Bundle(bundle)
    }
}

fun Bundle?._playbackExtras(): PlaybackExtras {
    return if (this != null) PlaybackExtras(this) else PlaybackExtras()
}

const val CMD_GET_EXOPLAYER = "cmd.get_exoplayer"
const val CMD_RESULT_OK = 1
const val CMD_RESULT_ARG1 = "arg1"

const val SEEK_DELTA_DURATION = 10000

class PlaybackSession
@Inject
constructor(
        private val mContext: Context,
        private val mSettings: VideoAppPreferences,
        private val mDbClient: VideosProviderClient,
        private val mQueue: PlaybackQueue,
        private val mDataService: DataService,
        private val mRenderer: ExoPlayerRenderer
) : MediaSession.Callback(), ExoPlayer.EventListener {

    /**
     * Allows us to pass a reference to this class through a bundle
     * This is not valid for ipc.
     * We use this so we can comply with mediasession api instead of
     * doing out-of-band calls to set the surfaces
     */
    inner class SessionBinder: Binder() {
        val player: SimpleExoPlayer
            get() = mRenderer.player
    }


    private val mMediaSession: MediaSession = MediaSession(mContext, BuildConfig.APPLICATION_ID)
    private var mMainHandler: Handler = Handler(Looper.getMainLooper())
    private var mBinder = SessionBinder()

    var mPlaybackState: PlaybackState by Delegates.observable(PlaybackState.Builder().build(), { _,_,nv ->
        mMediaSession.setPlaybackState(nv)
    })

    init {

        mMediaSession.setCallback(this, mMainHandler)
        mMediaSession.setFlags(MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS)
        //TODO mediaButtons
        //TODO activity
        mMediaSession.isActive = true


    }

    /*
     * Start mediasession callback methods
     */

    override fun onCommand(command: String, args: Bundle?, cb: ResultReceiver?) {
        Timber.d("onCommand(%s)", command)
        when (command) {
            CMD_GET_EXOPLAYER -> {
                cb!!.send(CMD_RESULT_OK, bundle()._putBinder(CMD_RESULT_ARG1, mBinder))
            }
        }
    }

    override fun onMediaButtonEvent(mediaButtonIntent: Intent): Boolean {
        Timber.d("onMediaButtonEvent()")
        return super.onMediaButtonEvent(mediaButtonIntent)
    }

    override fun onPlay() {
        Timber.d("onPlay()")
        mMediaSession.isActive = true
    }

    override fun onPlayFromMediaId(mediaId: String, extras: Bundle?) {
        Timber.d("onPlayFromMediaId(%s)", mediaId)
        onPause()
        mQueue.clear()
        if (!isLikelyJson(mediaId)) {
            changeState(STATE_ERROR) {
                it.setErrorMessage("Invalid MediaId")
            }
            return
        }
        val mediaRef = newMediaRef(mediaId)
        val playbackExtras = extras._playbackExtras()
        when (mediaRef.kind) {
            UPNP_VIDEO -> {
                mDataService.getMediaItem(mediaRef).subscribe({ item ->
                    val meta = item._getMediaMeta()
                    changeState(STATE_BUFFERING)

                    mExoPlayer.prepare()
                    if (playbackExtras.resume) {
                        if (meta.lastPlaybackPosition > 0) {
                            mExoPlayer.see
                            mForceSeekDuringLoad = true
                            mSeekOnMedia = meta.lastPlaybackPosition
                        }
                    }
                    loadMediaItem(item)
                    onPlay()
                }, { t ->
                    onStop()
                    changeState(STATE_ERROR) {
                        it.setErrorMessage(t.message)
                    }
                })
            }
            UPNP_FOLDER -> {
                TODO()
            }
        }
    }

    override fun onPlayFromSearch(query: String, extras: Bundle) {
        Timber.d("onPlayFromSearch(%s)", query)
        TODO()
    }

    override fun onPlayFromUri(uri: Uri, extras: Bundle) {
        Timber.d("onPlayFromUri(%s)", uri)
        TODO()
    }

    override fun onSkipToQueueItem(id: Long) {
        Timber.d("onSkipToQueueItem(%d)", id)
        TODO()
    }

    override fun onPause() {
        Timber.d("onPause()")
        TODO()
    }

    override fun onSkipToNext() {
        Timber.d("onSkipToNext()")
        TODO()
    }

    override fun onSkipToPrevious() {
        Timber.d("onSkipToPrevious()")
        TODO()
    }

    override fun onFastForward() {
        Timber.d("onFastForward()")
        TODO()
    }

    override fun onRewind() {
        Timber.d("onRewind()")
        TODO()
    }

    override fun onStop() {
        Timber.d("onStop()")
        TODO()
    }

    override fun onSeekTo(pos: Long) {
        Timber.d("onSeekTo(%d)", pos)
        TODO()
    }

    override fun onSetRating(rating: Rating) {
        Timber.d("onSetRating(%s)", rating)
        TODO()
    }

    override fun onCustomAction(action: String, extras: Bundle?) {
        Timber.d("onCustomAction(%s)", action)
        TODO()
    }

    /*
     * End mediasession callback methods
     */


    /*
     * Start Exoplayer callbacks
     */
    override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters?) {
        TODO("not implemented")
    }

    override fun onTracksChanged(trackGroups: TrackGroupArray?, trackSelections: TrackSelectionArray?) {
        TODO("not implemented")
    }

    override fun onPlayerError(error: ExoPlaybackException?) {
        TODO("not implemented")
    }

    override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
        TODO("not implemented")
    }

    override fun onLoadingChanged(isLoading: Boolean) {
        TODO("not implemented")
    }

    override fun onPositionDiscontinuity() {
        TODO("not implemented")
    }

    override fun onTimelineChanged(timeline: Timeline?, manifest: Any?) {
        TODO("not implemented")
    }

    /*
     * End Exoplayer callbacks
     */

    fun changeState(state: Int) {
        changeState(state, {})
    }

    fun changeState(state: Int, opts: (PlaybackState.Builder) -> Unit) {
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
            if (mExoPlayer.isCurrentWindowSeekable) {
                actions = actions or ACTION_SEEK_TO
            }
        }
        val builder = PlaybackState.Builder()
                .setActions(actions)
                .setState(state, mExoPlayer.currentPosition, mExoPlayer.playbackParameters.speed)
                .setBufferedPosition(mExoPlayer.bufferedPosition)
        mQueue.getCurrent().subscribeIgnoreError({ builder.setActiveQueueItemId(it.queueId) })
        opts(builder)
        mPlaybackState = builder.build()
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

