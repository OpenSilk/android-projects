package org.opensilk.media.playback

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.session.PlaybackState.*
import android.net.Uri
import android.os.PowerManager
import org.opensilk.common.dagger.ForApplication
import rx.Observable
import rx.subjects.PublishSubject
import timber.log.Timber
import javax.inject.Inject
import kotlin.properties.Delegates

/**
 * Created by drew on 2/24/17.
 *
 * States used STATE_NONE, STATE_BUFFERING, STATE_PAUSED, STATE_PLAYING
 */
class DefaultRenderer
@Inject
constructor(
        @ForApplication private val mContext: Context
): MediaPlayer.OnPreparedListener, MediaPlayer.OnSeekCompleteListener,
        MediaPlayer.OnCompletionListener,
        AudioManager.OnAudioFocusChangeListener {

    companion object {
        // The volume we set the media player to when we lose audio focus, but are
        // allowed to reduce the volume instead of stopping playback.
        val VOLUME_DUCK = 0.2f
        // The volume we set the media player when we have audio focus.
        val VOLUME_NORMAL = 1.0f
    }

    protected var mCurrentPlayer: MediaPlayer = MediaPlayer()
    protected var mNextPlayer: MediaPlayer = MediaPlayer()
    private val mAudioManager: AudioManager = mContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val mAudioSessionId: Int
    private val mAudioBecomingNoisyIntentFilter: IntentFilter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
    private val mAudioBecomingNoisyReceiver: BroadcastReceiver = object: BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY == intent?.action) {
                Timber.d("Headphones disconnected")
                pause()
            }
        }
    }
    private var mCurrentPlayerState: Int by Delegates.observable(STATE_NONE) { p, old, new ->
        mCurrentPlayerStateChanges.onNext(new)
    }
    private var mNextPlayerState: Int = STATE_NONE
    private var mPlayOnFocusGain: Boolean = false
    private var mSeekOnPrepared: Int = -1
    private val mCurrentPlayerStateChanges: PublishSubject<Int> = PublishSubject.create()

    init {
        mAudioSessionId = mAudioManager.generateAudioSessionId()

        mCurrentPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC)
        mCurrentPlayer.setAudioAttributes(AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .setUsage(AudioAttributes.USAGE_MEDIA).build())
        mCurrentPlayer.setWakeMode(mContext, PowerManager.PARTIAL_WAKE_LOCK)
        mCurrentPlayer.audioSessionId = mAudioSessionId
        mCurrentPlayer.setOnPreparedListener(this)
        mCurrentPlayer.setOnSeekCompleteListener(this)
        mCurrentPlayer.setOnCompletionListener(this)

        mNextPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC)
        mNextPlayer.setAudioAttributes(AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .setUsage(AudioAttributes.USAGE_MEDIA).build())
        mNextPlayer.setWakeMode(mContext, PowerManager.PARTIAL_WAKE_LOCK)
        mNextPlayer.audioSessionId = mAudioSessionId
        mNextPlayer.setOnPreparedListener(this)
        mNextPlayer.setOnSeekCompleteListener(this)
        mNextPlayer.setOnCompletionListener(this)
    }

    fun loadMedia(uri: Uri, headers: Map<String, String>) {
        mCurrentPlayer.reset()
        mCurrentPlayerState = STATE_BUFFERING
        if (!headers.isEmpty()) {
            mCurrentPlayer.setDataSource(mContext, uri, headers)
        } else {
            mCurrentPlayer.setDataSource(mContext, uri)
        }
        // Starts preparing the media player in the background. When
        // it's done, it will call our OnPreparedListener (that is,
        // the onPrepared() method on this class, since we set the
        // listener to 'this'). Until the media player is prepared,
        // we *cannot* call start() on it!
        mCurrentPlayer.prepareAsync()
    }

    fun loadNextMedia(uri: Uri, headers: Map<String, String>) {
        mCurrentPlayer.setNextMediaPlayer(null)
        mNextPlayer.reset()
        mNextPlayerState = STATE_BUFFERING
        if (!headers.isEmpty()) {
            mNextPlayer.setDataSource(mContext, uri, headers)
        } else {
            mNextPlayer.setDataSource(mContext, uri)
        }
        mNextPlayer.prepareAsync()
    }

    fun play() {
        when (mCurrentPlayerState) {
            STATE_PAUSED -> {
                val focus = mAudioManager.requestAudioFocus(this,
                        AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
                if (focus == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                    mContext.registerReceiver(mAudioBecomingNoisyReceiver, mAudioBecomingNoisyIntentFilter)
                    mCurrentPlayerState = STATE_PLAYING
                    mCurrentPlayer.start()
                } else {
                    TODO("Failed to get audio focus")
                }
                mPlayOnFocusGain = false
            }
            STATE_BUFFERING -> {
                mPlayOnFocusGain = true
            }
            STATE_PLAYING -> {
                Timber.w("Ignoring play() already playing")
            }
            else -> {
                Timber.w("TODO ignoring play() nothing playing")
            }
        }
    }

    fun pause() {
        //we don't use state here in case it is wrong
        //we always want to pause even if our state is screwed up
        if (mCurrentPlayer.isPlaying) {
            mCurrentPlayerState = STATE_PAUSED
            mCurrentPlayer.pause()
            mContext.unregisterReceiver(mAudioBecomingNoisyReceiver)
            val granted = mAudioManager.abandonAudioFocus(this)
            if (granted != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                TODO("This has never happened before")
            }
        } else {
            Timber.w("TODO ignoring pause() nothing playing")
        }
        mPlayOnFocusGain = false
    }

    fun seekTo(position: Int) {
        when (mCurrentPlayerState) {
            STATE_PAUSED,
            STATE_PLAYING -> {
                mCurrentPlayerState = STATE_BUFFERING
                mCurrentPlayer.seekTo(position)
                mSeekOnPrepared = -1
            }
            STATE_BUFFERING -> {
                mSeekOnPrepared = position
            }
            else -> {
                Timber.w("TODO ignoring seek() nothing playing")
            }
        }
    }

    fun destroy() {
        mCurrentPlayer.reset()
        mCurrentPlayer.release()
        mNextPlayer.reset()
        mNextPlayer.release()
    }

    val state: Int
        get() = mCurrentPlayerState

    val stateChanges: Observable<Int>
        get() = mCurrentPlayerStateChanges

    override fun onAudioFocusChange(focusChange: Int) {
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                pause()
                mPlayOnFocusGain = focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                mCurrentPlayer.setVolume(VOLUME_DUCK, VOLUME_DUCK)
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                mCurrentPlayer.setVolume(VOLUME_NORMAL, VOLUME_NORMAL)
                if (mPlayOnFocusGain) {
                    play()
                }
                mPlayOnFocusGain = false
            }
        }
    }

    override fun onPrepared(mp: MediaPlayer?) {
        if (mp == mCurrentPlayer) {
            if (mSeekOnPrepared > 0) {
                mCurrentPlayer.seekTo(mSeekOnPrepared)
                mSeekOnPrepared = -1
            } else {
                mCurrentPlayerState = STATE_PAUSED
                if (mPlayOnFocusGain) {
                    play()
                }
            }
        }
        if (mp == mNextPlayer) {
            mNextPlayerState = STATE_PAUSED
            mCurrentPlayer.setNextMediaPlayer(mNextPlayer)
        }
    }

    override fun onSeekComplete(mp: MediaPlayer?) {
        if (mp == mCurrentPlayer) {
            mCurrentPlayerState = STATE_PAUSED
            if (mPlayOnFocusGain) {
                play()
            }
        }
        if (mp == mNextPlayer) {
            TODO("Should not be here")
        }
    }

    override fun onCompletion(mp: MediaPlayer?) {
        if (mp == mCurrentPlayer) {
            if (mNextPlayer.isPlaying) {
                mCurrentPlayerState = STATE_PLAYING
                val old = mCurrentPlayer
                mCurrentPlayer = mNextPlayer
                mNextPlayer = old
                mNextPlayer.reset()
            } else {
                mCurrentPlayerState = STATE_NONE
                mCurrentPlayer.reset()
                mNextPlayerState = STATE_NONE
                mNextPlayer.reset()
            }
        }
        if (mp == mNextPlayer) {
            TODO("Should not be here")
        }
    }
}