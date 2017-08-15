package org.opensilk.video

import android.app.PendingIntent
import android.arch.lifecycle.*
import android.content.*
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Bundle
import android.os.SystemClock
import android.text.format.DateFormat
import android.view.SurfaceView
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.text.Cue
import com.google.android.exoplayer2.text.TextRenderer
import io.reactivex.disposables.Disposables
import org.opensilk.dagger2.ForApp
import org.opensilk.media.MediaId
import org.opensilk.media.bundle
import org.opensilk.media.playback.*
import timber.log.Timber
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class PlaybackViewModel
@Inject constructor(
        @ForApp private val mContext: Context,
        private val mPlaybackSession: PlaybackSession
) : ViewModel(), LifecycleObserver, MediaControllerCallback.Listener,
        SimpleExoPlayer.VideoListener, TextRenderer.Output {

    private val mExoPlayer = mPlaybackSession.player
    private val mTransportControls = mPlaybackSession.controller.transportControls

    private val mPlaybackState: PlaybackState
            get() = mPlaybackSession.controller.playbackState ?: PlaybackState.Builder().build()

    private val mMediaMetadata: MediaMetadata?
            get() = mPlaybackSession.controller.metadata

    private val mMediaControllerCallback = MediaControllerCallback(this)

    init {
        mPlaybackSession.controller.registerCallback(mMediaControllerCallback)
        mPlaybackSession.session.setMediaButtonReceiver(null) //recommends by android
        mPlaybackSession.setVideoMode()
        mExoPlayer.setVideoListener(this)
        mExoPlayer.setTextOutput(this)
        mTransportControls.sendCustomAction(ACTION_SET_REPEAT, bundle(KEY_REPEAT, VAL_REPEAT_OFF))
    }

    fun onMediaRef(mediaId: MediaId, playbackExtras: PlaybackExtras, activityComponent: ComponentName) {
        mTransportControls.playFromMediaId(mediaId.json, playbackExtras.bundle())

        val intent = Intent().setComponent(activityComponent)
                .setAction(ACTION_RESUME).putExtra(EXTRA_MEDIAID, mediaId.json)
        val pendingIntent = PendingIntent.getActivity(mContext, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT)
        mPlaybackSession.session.setSessionActivity(pendingIntent)
    }

    val systemTime = MutableLiveData<String>()

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun registerTimeTick() {
        systemTime.value = DateFormat.getTimeFormat(mContext).format(Date())
        mContext.registerReceiver(timetick, IntentFilter(Intent.ACTION_TIME_TICK))
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun unregisterTimeTick() {
        mContext.unregisterReceiver(timetick)
    }

    private val timetick = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            systemTime.value = DateFormat.getTimeFormat(context).format(Date())
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopProgressRunner()
        mPlaybackSession.release()
    }

    /*
     * start mediasession stuffs
     */

    fun pausePlayback() {
        mTransportControls.pause()
    }

    fun skipAhead() {
        if (mPlaybackState.hasAction(PlaybackState.ACTION_SEEK_TO)) {
            val offset = SystemClock.elapsedRealtime() - mPlaybackState.lastPositionUpdateTime
            val seek = mPlaybackState.position + offset + SKIP_AHEAD_MS
            mTransportControls.seekTo(seek)
        }
    }

    fun skipBehind() {
        if (mPlaybackState.hasAction(PlaybackState.ACTION_SEEK_TO)) {
            val offset = SystemClock.elapsedRealtime() - mPlaybackState.lastPositionUpdateTime
            val seek = mPlaybackState.position + offset - SKIP_BEHIND_MS
            mTransportControls.seekTo(seek)
        }
    }

    fun attachSurface(view: SurfaceView) {
        mExoPlayer.setVideoSurfaceView(view)
    }

    fun detachSurface(view: SurfaceView) {
        mExoPlayer.clearVideoSurfaceView(view)
    }

    fun togglePlayPause() {
        if (mPlaybackState.hasAction(PlaybackState.ACTION_PLAY)) {
            mTransportControls.play()
        } else {
            mTransportControls.pause()
        }
    }

    fun skipPrevious() {
        if (mPlaybackState.hasAction(PlaybackState.ACTION_SKIP_TO_PREVIOUS)) {
            mTransportControls.skipToPrevious()
        }
    }

    fun skipNext() {
        if (mPlaybackState.hasAction(PlaybackState.ACTION_SKIP_TO_NEXT)) {
            mTransportControls.skipToNext()
        }
    }

    /*
     * start media session callback
     */

    val videoDescription = MutableLiveData<VideoDescInfo>()
    val playbackTotalTimeSeconds = MutableLiveData<Long>()
    val playbackState = MutableLiveData<PlaybackState>()

    override fun onExtrasChanged(extras: Bundle) {
    }

    override fun onSessionEvent(event: String, extras: Bundle) {
    }

    override fun onQueueChanged(queue: List<MediaSession.QueueItem>) {
        Timber.d("onQueueChanged(size=${queue.size}")
    }

    override fun onQueueTitleChanged(title: String) {
    }

    override fun onPlaybackStateChanged(state: PlaybackState) {
        Timber.d("onPlaybackStateChanged(%s)", state._stringify())
        playbackState.postValue(state)
    }

    override fun onMetadataChanged(metadata: MediaMetadata) {
        Timber.d("onMetadataChanged(%s)", metadata.description.title)
        videoDescription.postValue(VideoDescInfo(
                metadata.description?.title?.toString() ?: "",
                metadata.description?.subtitle?.toString() ?: "",
                metadata.description?.description?.toString() ?: ""
        ))
        playbackTotalTimeSeconds.postValue(
                (metadata.getLong(MediaMetadata.METADATA_KEY_DURATION)) / 1000
        )
    }

    override fun onSessionDestroyed() {
        Timber.e("onSessionDestroyed()")
        //we should be unregistered before destroy is called
    }

    override fun onAudioInfoChanged(info: MediaController.PlaybackInfo) {
        Timber.d("onAudioInfoChanged")
    }

    /*
     * end media session callback
     */

    /*
     * Start exoplayer callbacks
     */

    val aspectRatio = MutableLiveData<Float>()
    val captions = MutableLiveData<List<Cue>>()

    override fun onVideoSizeChanged(width: Int, height: Int, unappliedRotationDegrees: Int, pixelWidthHeightRatio: Float) {
        val aspectRatio: Float = if (height == 0) 1f else (width.toFloat() * pixelWidthHeightRatio) / height.toFloat()
        this.aspectRatio.postValue(aspectRatio)
    }

    override fun onRenderedFirstFrame() {

    }

    override fun onCues(cues: MutableList<Cue>?) {
        captions.postValue(cues)
    }

    /*
     * end exoplayer callbacks
     */

    val playbackCurrentTimeSeconds = MutableLiveData<Long>()
    val playbackCurrentProgress = MutableLiveData<Int>()

    private var mProgressDisposable = Disposables.disposed()
    private val mProgressRunner = Runnable {
        val current = mPlaybackState.position + if (mPlaybackState.state == PlaybackState.STATE_PLAYING) {
            (SystemClock.elapsedRealtime() - mPlaybackState.lastPositionUpdateTime)
        } else 0L
        val duration = mMediaMetadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L

        // update progress
        if (duration == 0L || duration < current) {
            playbackCurrentProgress.postValue(0)
        } else {
            //permyriad calculation (no floating point)
            playbackCurrentProgress.postValue(((1000*current + duration/2)/duration).toInt())
        }
        //update current time text
        playbackCurrentTimeSeconds.postValue(current / 1000L)
    }

    fun startProgressRunner() {
        mProgressDisposable.dispose()
        mProgressDisposable = AppSchedulers.background
                .schedulePeriodicallyDirect(mProgressRunner, 0, 500, TimeUnit.MILLISECONDS)
    }

    fun stopProgressRunner() {
        mProgressDisposable.dispose()
    }

}