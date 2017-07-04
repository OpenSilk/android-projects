package org.opensilk.media.playback

import android.content.Context
import android.content.Intent
import android.media.MediaMetadata
import android.media.Rating
import android.media.browse.MediaBrowser
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.media.session.PlaybackState.*
import android.net.Uri
import android.os.*
import android.service.media.MediaBrowserService
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import org.opensilk.common.dagger.ForApplication
import org.opensilk.common.rx.subscribeIgnoreError
import org.opensilk.media.*
import timber.log.Timber
import java.util.*
import javax.inject.Inject
import kotlin.properties.Delegates

/**
 * Created by drew on 6/26/17.
 */
class PlaybackSession
@Inject
constructor(
        @ForApplication private val mContext: Context,
        private val mDbClient: MediaProviderClient,
        private val mQueue: PlaybackQueue,
        private val mRenderer: ExoPlayerRenderer
) : MediaSession.Callback() {

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

    private val mDataSourceFactory = DefaultDataSourceFactory(mContext,
            mContext.packageName + "/" + BuildConfig.VERSION_NAME)
    private val mExtractorFactory = DefaultExtractorsFactory()

    private var mPlaybackState: PlaybackState by Delegates.observable(PlaybackState.Builder().build(), { _, _, nv ->
        mMediaSession.setPlaybackState(nv)
    })

    val token: MediaSession.Token
        get() = mMediaSession.sessionToken

    init {
        mMediaSession.setCallback(this, mMainHandler)
        mMediaSession.setFlags(MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS)
        //TODO mediaButtons
        //TODO activity
        mMediaSession.isActive = true

        //subscribe to renderer changes
        mRenderer.stateChanges.subscribe {
            updateState(it)
        }

    }

    fun release() {
        mMediaSession.isActive = false
        mMediaSession.release()

        mRenderer.release()


    }

    private fun newMediaSource(uri: Uri): MediaSource {
        return ExtractorMediaSource(uri, mDataSourceFactory, mExtractorFactory, null, null)
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
        mRenderer.play()
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
                mDbClient.getMediaItem(mediaRef).subscribe({ item ->
                    val meta = item._getMediaMeta()
                    mRenderer.prepare(newMediaSource(meta.mediaUri))
                    if (playbackExtras.resume && meta.lastPlaybackPosition > 0) {
                        mRenderer.seekTo(meta.lastPlaybackPosition)
                    }
                    if (playbackExtras.playWhenReady) {
                        onPlay()
                    }
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
        mRenderer.pause()
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

    fun updateMetadata(media: MediaBrowser.MediaItem) {
        val meta = media._getMediaMeta()
        val bob = MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_MEDIA_ID, meta.mediaId)
                .putString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE, meta.title)
                .putString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE, meta.subtitle)
                .putLong(MediaMetadata.METADATA_KEY_DURATION, meta.duration)
        if (meta.artworkUri != Uri.EMPTY) {
            bob.putString(MediaMetadata.METADATA_KEY_DISPLAY_ICON_URI, meta.artworkUri.toString())
        }
        val desc = media.description
        if (desc.iconBitmap != null) {
            bob.putBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON, desc.iconBitmap)
        }
        mMediaSession.setMetadata(bob.build())
    }

    fun changeState(state: Int, opts: (PlaybackState.Builder) -> Unit) {
        val builder = PlaybackState.Builder()
        builder.setActions(generateActions(state))
        applyCommonState(builder)
        opts(builder)
        mPlaybackState = builder.build()
    }

    fun updateState(current: PlaybackState) {
        val builder = current._newBuilder()
        builder.setActions(generateActions(current.state))
        applyCommonState(builder)
        mPlaybackState = builder.build()
    }

    fun applyCommonState(builder: PlaybackState.Builder) {
        mQueue.getCurrent().subscribeIgnoreError({ builder.setActiveQueueItemId(it.queueId) })
    }

    fun generateActions(state: Int): Long {
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
        return actions
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