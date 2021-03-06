package org.opensilk.media.playback

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.media.MediaDescription
import android.media.MediaMetadata
import android.media.Rating
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.media.session.PlaybackState.*
import android.net.Uri
import android.os.*
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.ext.okhttp.OkHttpDataSourceFactory
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.source.UnrecognizedInputFormatException
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.Consumer
import io.reactivex.schedulers.Schedulers
import okhttp3.OkHttpClient
import org.opensilk.dagger2.ForApp
import org.opensilk.media.*
import org.opensilk.media.database.MediaDAO
import org.opensilk.reactivex2.subscribeIgnoreError
import timber.log.Timber
import javax.inject.Inject
import kotlin.properties.Delegates

/**
 * Created by drew on 6/26/17.
 */
class PlaybackSession
@Inject
constructor(
        @ForApp private val mContext: Context,
        private val mDbClient: MediaDAO,
        private val mQueue: PlaybackQueue,
        okHttpClient: OkHttpClient
) : MediaSession.Callback(), AudioManager.OnAudioFocusChangeListener, Player.EventListener {

    private val mMediaSession: MediaSession = MediaSession(mContext, BuildConfig.APPLICATION_ID)
    private val mDataSourceFactory = DefaultDataSourceFactory(mContext,
            null, OkHttpDataSourceFactory(okHttpClient, "${mContext.packageName}/ExoPlayer", null))
    private val mExtractorFactory = DefaultExtractorsFactory()
    private var mPlaybackState: PlaybackState by Delegates.observable(PlaybackState.Builder().build(), { _, _, nv ->
        mMediaSession.setPlaybackState(nv)
    })
    private val mTrackSelector: DefaultTrackSelector = DefaultTrackSelector()
    private val mRenderersFactory = ExoRenderersFactory(mContext)
    private var mExoPlayer: SimpleExoPlayer = ExoPlayerFactory.newSimpleInstance(mRenderersFactory, mTrackSelector)
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
    private val mBackgroundScheduler = Schedulers.single()
    private val mMainScheduler = AndroidSchedulers.mainThread()

    val session: MediaSession
        get() = mMediaSession

    val token: MediaSession.Token
        get() = mMediaSession.sessionToken

    val controller: MediaController
        get() = mMediaSession.controller

    val player: SimpleExoPlayer
        get() = mExoPlayer

    fun setVideoMode() {
        mExoPlayer.audioAttributes = AudioAttributes.Builder()
                .setContentType(C.CONTENT_TYPE_MOVIE)
                .setUsage(C.USAGE_MEDIA)
                .build()
        mTrackSelector.setTunnelingAudioSessionId(C.generateAudioSessionIdV21(mContext))
    }

    init {
        mExoPlayer.addListener(this)
        val eventLogger = EventLogger(mTrackSelector)
        mExoPlayer.addListener(eventLogger)
        mExoPlayer.setAudioDebugListener(eventLogger)
        mExoPlayer.setVideoDebugListener(eventLogger)
        mExoPlayer.setMetadataOutput(eventLogger)
        mExoPlayer.audioAttributes = AudioAttributes.Builder()
                .setContentType(C.CONTENT_TYPE_MUSIC)
                .setUsage(C.USAGE_MEDIA).build()

        mWakeLock.setReferenceCounted(false)

        mMediaSession.setCallback(this)
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

    private fun Int._stringifyAudioFocusChange(): String? {
        return AudioManager::class.java.declaredFields.filter {
            it.name.startsWith("AUDIOFOCUS_") && it.type == Int::class.java && it.get(null) == this
        }.firstOrNull()?.name
    }

    override fun onAudioFocusChange(focusChange: Int) {
        Timber.d("onAudioFocusChange(${focusChange._stringifyAudioFocusChange()})")
        if (focusChange < 0) { //focus lost
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
                    mPlayOnFocusGain = true
                }
                AudioManager.AUDIOFOCUS_GAIN -> {

                }
            }
        } else { //focusgain
            mExoPlayer.volume = VOLUME_NORMAL
            mExoPlayer.playWhenReady = mPlayOnFocusGain
            mPlayOnFocusGain = false
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

    override fun onPlayerError(error: ExoPlaybackException) {
        stop()
        changeState(PlaybackState.STATE_ERROR) {
            when (error.cause) {
                is UnrecognizedInputFormatException -> {
                    it.setErrorMessage("Unsupported media format")
                }
                else -> {
                    it.setErrorMessage(error.cause?.message ?: "ExoPlayer encountered an error.")
                }
            }
        }
    }

    private fun Int._stringifyExoPlayerState() = when (this) {
        Player.STATE_BUFFERING -> "STATE_BUFFERING"
        Player.STATE_IDLE -> "STATE_IDLE"
        Player.STATE_ENDED -> "STATE_ENDED"
        Player.STATE_READY -> "STATE_READY"
        else -> "UNKNOWN"
    }

    override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
        Timber.d("onExoPlayerStateChanged(playWhenReady=%s, playbackState=%s",
                playWhenReady, playbackState._stringifyExoPlayerState())
        when (playbackState) {
            Player.STATE_IDLE -> {
                //changeState(PlaybackState.STATE_NONE)
            }
            Player.STATE_BUFFERING -> {
                changeState(STATE_BUFFERING)
            }
            Player.STATE_ENDED -> {
                //update pos on last played
                mQueue.getCurrent().subscribeIgnoreError(Consumer { item ->
                    val ref = item.description.mediaId.toMediaId()
                    mDbClient.setLastPlaybackPosition(ref, mExoPlayer.duration, mExoPlayer.duration)
                })
                mQueue.goToNext().subscribe({ item ->
                    prepareMedia(item.description._getMediaUri())
                    mExoPlayer.playWhenReady = true
                    updateMetadata(item.description)
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
            Player.STATE_READY -> {
                if (playWhenReady) {
                    changeState(PlaybackState.STATE_PLAYING)
                } else {
                    changeState(PlaybackState.STATE_PAUSED)
                }
                val rendererDuration = player.duration
                val metaDuration = controller.metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0
                if (rendererDuration != metaDuration) {
                    mQueue.getCurrent().subscribeIgnoreError(Consumer { item ->
                        item.description.extras.putLong(KEY_DURATION, rendererDuration)
                        updateMetadata(item.description)
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
        saveCurrentPosition()
    }

    override fun onTimelineChanged(timeline: Timeline, manifest: Any?) {
        if (timeline.isEmpty) return
        //TODO ?? what changes
    }

    override fun onRepeatModeChanged(repeatMode: Int) {

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
        val mediaRef = mediaId.toMediaId()
        val playbackExtras = extras._playbackExtras()
        when (mediaRef) {
            is VideoId -> fetchAndPlaySiblingVideos(mediaRef, playbackExtras)
            else -> stopAndShowError("Unsupported media kind=${mediaRef.javaClass.name}")
        }
    }

    private fun fetchAndPlaySiblingVideos(mediaId: VideoId, playbackExtras: PlaybackExtras) {
        mDbClient.playableSiblingVideos(mediaId).toList()
                .subscribeOn(mBackgroundScheduler).observeOn(mMainScheduler).subscribe({ metaList ->
            //handle last playback position
            val currentRef = metaList.first {
                it.id == mediaId
            }
            val resumeInfo = currentRef.resumeInfo
            val lastPlaybackPosition = if (resumeInfo != null && playbackExtras.resume) {
                if (resumeInfo.lastCompletion < PRETTY_MUCH_COMPLETE) {
                    resumeInfo.lastPosition
                } else 0
            } else 0

            //populate the queue
            metaList.forEach {
                mQueue.add(it.toMediaDescription())
            }
            val queue = mQueue.get()
            val currentQueueItem = queue.first {
                it.description.mediaId.toMediaId() == mediaId
            }
            mQueue.setCurrent(currentQueueItem.queueId)
            mMediaSession.setQueue(queue)

            //play it
            prepareMedia(currentQueueItem.description._getMediaUri(), lastPlaybackPosition)
            if (playbackExtras.playWhenReady) {
                play()
            }
            updateMetadata(currentQueueItem.description)

        }, { t ->
            stop()
            changeState(STATE_ERROR) {
                it.setErrorMessage(t.message)
            }
        })
    }

    private fun prepareMedia(mediaUri: Uri, lastPlaybackPosition: Long = 0) {
        //assemble mediasource
        val mediaSource = newMediaSource(mediaUri)
        mExoPlayer.prepare(mediaSource)
        if (lastPlaybackPosition > 0) {
            mExoPlayer.seekTo(lastPlaybackPosition)
        }
    }

    private fun saveCurrentPosition() {
        //clear last pos for current item
        mQueue.getCurrent().observeOn(mBackgroundScheduler).subscribeIgnoreError(Consumer { item ->
            val ref = item.description.mediaId.toMediaId()
            mDbClient.setLastPlaybackPosition(ref, mExoPlayer.currentPosition, mExoPlayer.duration)
        })
    }

    private fun stopAndShowError(msg: String) {
        stop()
        changeState(STATE_ERROR) {
            it.setErrorMessage(msg)
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
        saveCurrentPosition()
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
        saveCurrentPosition()
        mQueue.goToNext().subscribe({
            pause()
            prepareMedia(it.description._getMediaUri())
            play()
            updateMetadata(it.description)
        }, { error ->
            stop()
            changeState(STATE_ERROR) {
                it.setErrorMessage(error.message)
            }
        })
    }

    override fun onSkipToPrevious() {
        Timber.d("onSkipToPrevious()")
        saveCurrentPosition()
        mQueue.goToPrevious().subscribe({
            pause()
            prepareMedia(it.description._getMediaUri())
            play()
            updateMetadata(it.description)
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
        saveCurrentPosition()
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

    private fun updateMetadata(desc: MediaDescription) {
        val bob = MediaMetadata.Builder()
        bob.putString(MediaMetadata.METADATA_KEY_MEDIA_ID, desc.mediaId)
                .putText(MediaMetadata.METADATA_KEY_TITLE, desc.title)
                .putText(MediaMetadata.METADATA_KEY_DISPLAY_TITLE, desc.title)
                .putText(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE, desc.subtitle)
                .putLong(MediaMetadata.METADATA_KEY_DURATION, desc.extras.getLong(KEY_DURATION, 0))
        if (!desc.iconUri.isEmpty()) {
            bob.putString(MediaMetadata.METADATA_KEY_DISPLAY_ICON_URI, desc.iconUri.toString())
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
        return when (state) {
            STATE_PLAYING,
            STATE_BUFFERING -> ACTION_PAUSE or getSeekableAction() or getQueueActions()

            STATE_PAUSED -> ACTION_PLAY or getSeekableAction() or getQueueActions()

            STATE_SKIPPING_TO_NEXT,
            STATE_SKIPPING_TO_PREVIOUS,
            STATE_SKIPPING_TO_QUEUE_ITEM -> ACTION_PAUSE

            STATE_FAST_FORWARDING,
            STATE_REWINDING -> ACTION_PLAY or ACTION_PAUSE

            STATE_ERROR,
            STATE_STOPPED,
            STATE_NONE -> 0

            else -> 0
        } or ACTION_PLAY_FROM_MEDIA_ID
    }

    private fun getSeekableAction(): Long {
        return if (mExoPlayer.isCurrentWindowSeekable) {
            ACTION_SEEK_TO
        } else 0L
    }

    private fun getQueueActions(): Long {
        return if (mQueue.hasNext()) {
            ACTION_SKIP_TO_NEXT
        } else 0L or if (mQueue.hasPrevious()) {
            ACTION_SKIP_TO_PREVIOUS
        } else 0L or if (mQueue.notEmpty()) {
            ACTION_SKIP_TO_QUEUE_ITEM
        } else 0L
    }

}
