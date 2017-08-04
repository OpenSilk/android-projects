package org.opensilk.media.playback

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.Rating
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.media.session.PlaybackState.*
import android.net.Uri
import android.os.*
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import io.reactivex.Single
import io.reactivex.functions.BiFunction
import io.reactivex.functions.Consumer
import io.reactivex.subjects.BehaviorSubject
import org.opensilk.common.dagger.ForApplication
import org.opensilk.common.rx.subscribeIgnoreError
import org.opensilk.media.*
import timber.log.Timber
import javax.inject.Inject
import kotlin.properties.Delegates

const val ACTION_SET_REPEAT = "org.opensilk.media.ACTION_SET_REPEAT"
const val KEY_REPEAT = "org.opensilk.media.KEY_REPEAT"
const val VAL_REPEAT_OFF = 1
const val VAL_REPEAT_ON = 2 //default

// The volume we set the media player to when we lose audio focus, but are
// allowed to reduce the volume instead of stopping playback.
internal const val VOLUME_DUCK = 0.2f
// The volume we set the media player when we have audio focus.
internal const val VOLUME_NORMAL = 1.0f

/**
 * Created by drew on 6/26/17.
 */
class PlaybackSession
@Inject
constructor(
        @ForApplication private val mContext: Context,
        private val mDbClient: MediaProviderClient,
        private val mQueue: PlaybackQueue
) : MediaSession.Callback(), AudioManager.OnAudioFocusChangeListener, ExoPlayer.EventListener {

    private val mMediaSession: MediaSession = MediaSession(mContext, BuildConfig.APPLICATION_ID)
    private var mMainHandler: Handler = Handler(Looper.getMainLooper())
    private val mDataSourceFactory = DefaultDataSourceFactory(mContext,
            mContext.packageName + "/" + BuildConfig.VERSION_NAME)
    private val mExtractorFactory = DefaultExtractorsFactory()
    private var mPlaybackState: PlaybackState by Delegates.observable(PlaybackState.Builder().build(), { _, _, nv ->
        mMediaSession.setPlaybackState(nv)
    })
    private val mTrackSelector: DefaultTrackSelector = DefaultTrackSelector()
    private var mExoPlayer: SimpleExoPlayer = ExoPlayerFactory.newSimpleInstance(
            DefaultRenderersFactory(mContext, null, DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON),
            mTrackSelector)
    private val mAudioManager: AudioManager = mContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val mAudioBecomingNoisyIntentFilter: IntentFilter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
    private val mAudioBecomingNoisyReceiver: BroadcastReceiver = object: BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY == intent?.action) {
                Timber.d("Headphones disconnected")
                mExoPlayer.playWhenReady = false
            }
        }
    }
    private var mAudioBecomingNoisyRegistered = false
    private var mPlayOnFocusGain: Boolean = false
    private val mWakeLock: PowerManager.WakeLock = (mContext.getSystemService(Context.POWER_SERVICE)
            as PowerManager).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, mContext.packageName)


    val session: MediaSession
        get() = mMediaSession

    val token: MediaSession.Token
        get() = mMediaSession.sessionToken

    val controller: MediaController
        get() = mMediaSession.controller

    val player: SimpleExoPlayer
        get() = mExoPlayer

    init {
        mExoPlayer.addListener(this)
        val eventLogger = EventLogger(mTrackSelector)
        mExoPlayer.addListener(eventLogger)
        mExoPlayer.setAudioDebugListener(eventLogger)
        mExoPlayer.setVideoDebugListener(eventLogger)
        mExoPlayer.setMetadataOutput(eventLogger)

        mWakeLock.setReferenceCounted(false)

        mMediaSession.setCallback(this, mMainHandler)
        mMediaSession.setFlags(MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS)
        //TODO mediaButtons
    }

    fun release() {
        stop()
        mPlaybackState = PlaybackState.Builder().build() //STATE_NONE
        mMediaSession.release()
        mExoPlayer.release()
    }

    private fun newMediaSource(uri: Uri): MediaSource {
        return ExtractorMediaSource(uri, mDataSourceFactory, mExtractorFactory, null, null)
    }

    /*
     * AudioManager listener
     */

    override fun onAudioFocusChange(focusChange: Int) {
        Timber.d("onAudioFocusChange()")
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                mPlayOnFocusGain = false
                if (mExoPlayer.playWhenReady) {
                    onPause()
                    mPlayOnFocusGain = focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT
                }
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                mExoPlayer.volume = VOLUME_DUCK
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                mExoPlayer.volume = VOLUME_NORMAL
                mExoPlayer.playWhenReady = mPlayOnFocusGain
                mPlayOnFocusGain = false
            }
        }
    }

    /*
     * ExoPlayer listener
     */

    override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
        if (playbackParameters.speed != mPlaybackState.playbackSpeed) {
            updateState(mPlaybackState)
        }
    }

    override fun onTracksChanged(trackGroups: TrackGroupArray?, trackSelections: TrackSelectionArray?) {

    }

    override fun onPlayerError(error: ExoPlaybackException?) {
        stop()
        changeState(PlaybackState.STATE_ERROR) {
            it.setErrorMessage(error?.message ?: "ExoPlayer encountered an error.")
        }
    }

    fun Int._stringifyExoPlayerState(): String {
        return when (this) {
            ExoPlayer.STATE_BUFFERING -> "STATE_BUFFERING"
            ExoPlayer.STATE_IDLE -> "STATE_IDLE"
            ExoPlayer.STATE_ENDED -> "STATE_ENDED"
            ExoPlayer.STATE_READY -> "STATE_READY"
            else -> "UNKNOWN"
        }
    }

    override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
        Timber.d("onExoPlayerStateChanged(playWhenReady=%s, playbackState=%s",
                playWhenReady, playbackState._stringifyExoPlayerState())
        when (playbackState) {
            ExoPlayer.STATE_IDLE -> {
                //changeState(PlaybackState.STATE_NONE)
            }
            ExoPlayer.STATE_BUFFERING -> {
                changeState(STATE_BUFFERING)
            }
            ExoPlayer.STATE_ENDED -> {
                //update pos on last played
                mQueue.getCurrent().subscribeIgnoreError(Consumer { item ->
                    val ref = newMediaRef(item.description.mediaId)
                    mDbClient.setLastPlaybackPosition(ref, mExoPlayer.duration, mExoPlayer.duration)
                })
                mQueue.goToNext().subscribe({ item ->
                    val meta = item.description._getMediaMeta()
                    prepareMedia(meta)
                    mExoPlayer.playWhenReady = true
                    updateMetadata(meta)
                }, { error ->
                    stop()
                    changeState(STATE_ERROR) {
                        it.setErrorMessage(error.message)
                    }
                }, {
                    //end of the line
                    stop()
                    changeState(STATE_STOPPED)
                })
            }
            ExoPlayer.STATE_READY -> {
                if (playWhenReady) {
                    changeState(PlaybackState.STATE_PLAYING)
                } else {
                    changeState(PlaybackState.STATE_PAUSED)
                }
                val rendererDuration = player.duration
                val metaDuration = controller.metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0
                if (rendererDuration != metaDuration) {
                    mQueue.getCurrent().subscribeIgnoreError(Consumer { item ->
                        val meta = item.description._getMediaMeta()
                        meta.duration = rendererDuration
                        updateMetadata(meta)
                    })
                }
            }
        }
    }

    override fun onLoadingChanged(isLoading: Boolean) {
        //pass
    }

    override fun onPositionDiscontinuity() {
        changeState(mPlaybackState.state)
    }

    override fun onTimelineChanged(timeline: Timeline, manifest: Any?) {
        if (timeline.isEmpty) return
        //TODO ?? what changes
    }

    /*
     * Start mediasession callback methods
     */

    override fun onCommand(command: String, args: Bundle?, cb: ResultReceiver?) {
        Timber.d("onCommand(%s)", command)
    }

    override fun onMediaButtonEvent(mediaButtonIntent: Intent): Boolean {
        Timber.d("onMediaButtonEvent()")
        return super.onMediaButtonEvent(mediaButtonIntent)
    }

    override fun onPlay() {
        Timber.d("onPlay()")
        if (mExoPlayer.playWhenReady) {
            Timber.i("Ignoring duplicate play() call")
            return
        }
        play()
    }

    private fun play() {
        mMediaSession.isActive = true
        mWakeLock.acquire()
        val focus = mAudioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
        when (focus) {
            AudioManager.AUDIOFOCUS_REQUEST_GRANTED -> {
                if (!mAudioBecomingNoisyRegistered) {
                    mAudioBecomingNoisyRegistered = true
                    mContext.registerReceiver(mAudioBecomingNoisyReceiver, mAudioBecomingNoisyIntentFilter)
                }
                mExoPlayer.playWhenReady = true
            }
            else -> {
                mExoPlayer.playWhenReady = false
                mWakeLock.release()
                changeState(STATE_ERROR)
            }
        }
    }

    private class MetaWithPos(val meta: MediaMeta, val pos: Long)

    override fun onPlayFromMediaId(mediaId: String, extras: Bundle?) {
        Timber.d("onPlayFromMediaId(%s)", mediaId)
        pause()
        mQueue.clear()
        mMediaSession.setQueue(emptyList())
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
                Single.zip<MediaMeta, Long, MetaWithPos>(
                        mDbClient.siblingsOf(mediaRef).doOnNext {
                            //everyone gets added to the queue
                            mQueue.add(it)
                        }.skipWhile {
                            //but we only want to load ourselves
                            newMediaRef(it.mediaId) != mediaRef
                        }.firstOrError(),
                        //get playback position for resume
                        mDbClient.getLastPlaybackPosition(mediaRef).onErrorReturn { 0 },
                        BiFunction { list, pos -> MetaWithPos(list, pos) }
                ).subscribe({ mwp ->
                    val meta = mwp.meta
                    val lastPlaybackPosition = if (playbackExtras.resume) mwp.pos else 0
                    //fixup the queue
                    mQueue.get().first { newMediaRef(it.description.mediaId) == mediaRef }.let {
                        mQueue.goToItem(it.queueId)
                    }
                    mMediaSession.setQueue(mQueue.get())
                    prepareMedia(meta, lastPlaybackPosition)
                    if (playbackExtras.playWhenReady) {
                        play()
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

    private fun prepareMedia(meta: MediaMeta, lastPlaybackPosition: Long = 0) {
        //update meta
        updateMetadata(meta)
        //assemble mediasource
        val mediaSource = newMediaSource(meta.mediaUri)
        mExoPlayer.prepare(mediaSource)
        if (lastPlaybackPosition > 0) {
            mExoPlayer.seekTo(lastPlaybackPosition)
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
        if (!mExoPlayer.playWhenReady) {
            Timber.i("Ignoring duplicate pause() call")
            return
        }
        //save current pos
        mQueue.getCurrent().subscribeIgnoreError(Consumer { item ->
            val ref = newMediaRef(item.description.mediaId)
            mDbClient.setLastPlaybackPosition(ref,
                    mExoPlayer.currentPosition, mExoPlayer.duration)
        })
        pause()
        changeState(STATE_PAUSED)
    }

    private fun pause() {
        mExoPlayer.playWhenReady = false
        if (mAudioBecomingNoisyRegistered) {
            mAudioBecomingNoisyRegistered = false
            mContext.unregisterReceiver(mAudioBecomingNoisyReceiver)
        }
        mWakeLock.release()
    }

    override fun onSkipToNext() {
        Timber.d("onSkipToNext()")
        //clear last pos for current item
        mQueue.getCurrent().subscribeIgnoreError(Consumer { item ->
            val ref = newMediaRef(item.description.mediaId)
            mDbClient.setLastPlaybackPosition(ref, 0, 1)
        })
        mQueue.goToNext().subscribe({
            val meta = it.description._getMediaMeta()
            pause()
            prepareMedia(meta)
            play()
            updateMetadata(meta)
        }, { error ->
            stop()
            changeState(STATE_ERROR) {
                it.setErrorMessage(error.message)
            }
        })
    }

    override fun onSkipToPrevious() {
        Timber.d("onSkipToPrevious()")
        //clear last pos for current item
        mQueue.getCurrent().subscribeIgnoreError(Consumer { item ->
            val ref = newMediaRef(item.description.mediaId)
            mDbClient.setLastPlaybackPosition(ref, 0, 1)
        })
        mQueue.goToPrevious().subscribe({
            val meta = it.description._getMediaMeta()
            pause()
            prepareMedia(meta)
            play()
            updateMetadata(meta)
        }, { error ->
            stop()
            changeState(STATE_ERROR) {
                it.setErrorMessage(error.message)
            }
        })
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
        //save current pos
        mQueue.getCurrent().subscribeIgnoreError(Consumer { item ->
            val ref = newMediaRef(item.description.mediaId)
            mDbClient.setLastPlaybackPosition(ref,
                    mExoPlayer.currentPosition, mExoPlayer.duration)
        })
        stop()
        changeState(STATE_STOPPED)
    }

    private fun stop() {
        pause()
        mAudioManager.abandonAudioFocus(this)
        mMediaSession.isActive = false
    }

    override fun onSeekTo(pos: Long) {
        Timber.d("onSeekTo(%d)", pos)
        if (pos < 0) {
            mExoPlayer.seekTo(C.TIME_UNSET)
        } else {
            mExoPlayer.seekTo(pos)
        }
    }

    override fun onSetRating(rating: Rating) {
        Timber.d("onSetRating(%s)", rating)
        TODO()
    }

    override fun onCustomAction(action: String, extras: Bundle?) {
        Timber.d("onCustomAction(%s)", action)
        when (action) {
            ACTION_SET_REPEAT -> {
                mQueue.setWrap(extras!!.getInt(KEY_REPEAT, VAL_REPEAT_ON) != VAL_REPEAT_OFF)
            }
        }
    }

    /*
     * End mediasession callback methods
     */

    private fun updateMetadata(meta: MediaMeta) {
        val bob = MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_MEDIA_ID, meta.mediaId)
                .putString(MediaMetadata.METADATA_KEY_TITLE, meta.title.elseIfBlank(meta.displayName))
                .putString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE, meta.title.elseIfBlank(meta.displayName))
                .putString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE, meta.subtitle)
                .putLong(MediaMetadata.METADATA_KEY_DURATION, meta.duration)
        if (meta.artworkUri != Uri.EMPTY) {
            bob.putString(MediaMetadata.METADATA_KEY_DISPLAY_ICON_URI, meta.artworkUri.toString())
        }
        //TODO load bitmap
        mMediaSession.setMetadata(bob.build())
    }

    private fun changeState(state: Int, opts: (PlaybackState.Builder) -> Unit = {}) {
        val builder = PlaybackState.Builder()
        builder.setActions(generateActions(state))
        applyCommonState(state, builder)
        opts(builder)
        mPlaybackState = builder.build()
    }

    private fun updateState(current: PlaybackState) {
        val builder = current._newBuilder()
        builder.setActions(generateActions(current.state))
        applyCommonState(current.state, builder)
        mPlaybackState = builder.build()
    }

    private fun applyCommonState(state: Int, builder: PlaybackState.Builder) {
        builder.setState(state, mExoPlayer.currentPosition, mExoPlayer.playbackParameters.speed)
        builder.setBufferedPosition(mExoPlayer.bufferedPosition)
        mQueue.getCurrent().subscribeIgnoreError(Consumer {
            builder.setActiveQueueItemId(it.queueId)
        })
    }

    private fun generateActions(state: Int): Long {
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
        if (!mExoPlayer.isCurrentWindowSeekable) {
            //disable seek if unsupported
            actions = actions and ACTION_SEEK_TO.inv()
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