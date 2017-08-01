package org.opensilk.media.playback

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.media.session.PlaybackState
import android.os.PowerManager
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import org.opensilk.common.dagger.ForApplication
import rx.Observable
import rx.subjects.BehaviorSubject
import timber.log.Timber
import javax.inject.Inject
import kotlin.properties.Delegates

/**
 * Created by drew on 6/24/17.
 */
class ExoPlayerRenderer
@Inject
constructor(
        @ForApplication private val mContext: Context
): AudioManager.OnAudioFocusChangeListener, ExoPlayer.EventListener {

    companion object {
        // The volume we set the media player to when we lose audio focus, but are
        // allowed to reduce the volume instead of stopping playback.
        val VOLUME_DUCK = 0.2f
        // The volume we set the media player when we have audio focus.
        val VOLUME_NORMAL = 1.0f
    }

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
    private var mPlayOnFocusGain: Boolean = false
    private val mWakeLock: PowerManager.WakeLock = (mContext.getSystemService(Context.POWER_SERVICE)
            as PowerManager).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, mContext.packageName)
    private val mStateChanges = BehaviorSubject.create<PlaybackState>()
    private var mPlaybackState: PlaybackState by Delegates.observable(PlaybackState.Builder().build(), { _, _, nv ->
        mStateChanges.onNext(nv)
    })
    private var mLoading = false

    init {
        mExoPlayer.addListener(this)
        val eventLogger = EventLogger(mTrackSelector)
        mExoPlayer.addListener(eventLogger)
        mExoPlayer.setAudioDebugListener(eventLogger)
        mExoPlayer.setVideoDebugListener(eventLogger)
        mExoPlayer.setMetadataOutput(eventLogger)

        mWakeLock.setReferenceCounted(false)

    }

    val player: SimpleExoPlayer
        get() = mExoPlayer

    val stateChanges: Observable<PlaybackState>
        get() = mStateChanges.asObservable()

    private fun changeState(state: Int) {
        changeState(state, {})
    }

    private fun changeState(state: Int, opts: (PlaybackState.Builder) -> Unit) {
        var actions: Long = 0L
        if (mExoPlayer.isCurrentWindowSeekable) {
            actions = actions or PlaybackState.ACTION_SEEK_TO
        }
        val builder = PlaybackState.Builder()
                .setActions(actions)
                .setState(state, mExoPlayer.currentPosition, mExoPlayer.playbackParameters.speed)
                .setBufferedPosition(mExoPlayer.bufferedPosition)
        opts(builder)
        mPlaybackState = builder.build()
    }

    fun prepare(mediaSource: MediaSource) {
        pause()
        mExoPlayer.prepare(mediaSource)
    }

    fun play() {
        if (mExoPlayer.playWhenReady) {
            Timber.i("Ignoring duplicate play() call")
            return
        }
        mWakeLock.acquire()
        val focus = mAudioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
        when (focus) {
            AudioManager.AUDIOFOCUS_REQUEST_GRANTED -> {
                mContext.registerReceiver(mAudioBecomingNoisyReceiver, mAudioBecomingNoisyIntentFilter)
                mExoPlayer.playWhenReady = true
            }
            else -> {
                mExoPlayer.playWhenReady = false
                mWakeLock.release()
                changeState(PlaybackState.STATE_ERROR)
                TODO("ERROR")
            }
        }
    }

    fun pause() {
        if (!mExoPlayer.playWhenReady) {
            Timber.i("Ignoring duplicate pause() call")
            return
        }
        mExoPlayer.playWhenReady = false
        mContext.unregisterReceiver(mAudioBecomingNoisyReceiver)
        mAudioManager.abandonAudioFocus(this)
        mWakeLock.release()
    }

    fun seekTo(pos: Long) {
        mExoPlayer.seekTo(pos)
    }

    fun release() {
        pause()
        mExoPlayer.stop()
        mExoPlayer.release()
    }

    /*
     * AudioManager listener
     */

    override fun onAudioFocusChange(focusChange: Int) {
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                pause()
                mPlayOnFocusGain = focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT
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
            changeState(mPlaybackState.state)
        }
    }

    override fun onTracksChanged(trackGroups: TrackGroupArray?, trackSelections: TrackSelectionArray?) {

    }

    override fun onPlayerError(error: ExoPlaybackException?) {
        changeState(PlaybackState.STATE_ERROR) {
            it.setErrorMessage(error?.message ?: "ExoPlayer encountered an error.")
        }
    }

    override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
        when (playbackState) {
            ExoPlayer.STATE_IDLE -> {
                changeState(PlaybackState.STATE_NONE)
            }
            ExoPlayer.STATE_BUFFERING -> {
                changeState(PlaybackState.STATE_BUFFERING)
            }
            ExoPlayer.STATE_ENDED -> {
                changeState(PlaybackState.STATE_STOPPED)
            }
            ExoPlayer.STATE_READY -> {
                if (playWhenReady) {
                    changeState(PlaybackState.STATE_PLAYING)
                } else {
                    changeState(PlaybackState.STATE_PAUSED)
                }
            }
        }
    }

    override fun onLoadingChanged(isLoading: Boolean) {
        mLoading = isLoading
    }

    override fun onPositionDiscontinuity() {
        changeState(mPlaybackState.state)
    }

    override fun onTimelineChanged(timeline: Timeline, manifest: Any?) {
        if (timeline.isEmpty) return
        //TODO ?? what changes
    }
}