package org.opensilk.video.telly

import android.animation.Animator
import android.app.PendingIntent
import android.arch.lifecycle.*
import android.content.*
import android.content.res.Configuration
import android.databinding.DataBindingUtil
import android.media.MediaMetadata
import android.media.browse.MediaBrowser
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.*
import android.text.format.DateFormat
import android.view.KeyEvent
import android.view.SurfaceView
import android.view.View
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.text.Cue
import com.google.android.exoplayer2.text.TextRenderer
import dagger.Binds
import dagger.Module
import dagger.Subcomponent
import dagger.multibindings.IntoMap
import io.reactivex.Scheduler
import io.reactivex.disposables.Disposables
import io.reactivex.functions.Consumer
import io.reactivex.schedulers.Schedulers
import org.opensilk.common.dagger.ActivityScope
import org.opensilk.common.dagger.ForApplication
import org.opensilk.common.dagger.Injector
import org.opensilk.media.*
import org.opensilk.media.playback.*
import org.opensilk.video.*
import org.opensilk.video.telly.databinding.ActivityPlaybackBinding
import timber.log.Timber
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject

const val ACTION_PLAY = "org.opensilk.action.PLAY"
const val ACTION_RESUME = "org.opensilk.action.RESUME"

const val EXTRA_PLAY_WHEN_READY = "org.opensilk.extra.PLAY_WHEN_READY"

const val OVERLAY_ANIM_DURATION = 300L
const val OVERLAY_SHOW_DURATION = 3000L

const val SKIP_AHEAD_MS = 10000
const val SKIP_BEHIND_MS = 6000

/**
 * Created by drew on 6/3/17.
 */
@ActivityScope
@Subcomponent
interface PlaybackComponent: Injector<PlaybackActivity> {
    @Subcomponent.Builder
    abstract class Builder: Injector.Builder<PlaybackActivity>()
}

/**
 *
 */
@Module(subcomponents = arrayOf(PlaybackComponent::class))
abstract class PlaybackModule {
    @Binds @IntoMap @ViewModelKey(PlaybackViewModel::class)
    abstract fun bindPlaybackViewModel(viewModel: PlaybackViewModel): ViewModel
}

/**
 *
 */
interface PlaybackActionsHandler {
    fun toggleCaptions()
    fun togglePlayPause()
    fun skipPrevious()
    fun skipNext()
    fun enterPip()
}

class PlaybackViewModel
@Inject constructor(
        @ForApplication private val mContext: Context,
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
        mExoPlayer.setVideoListener(this)
        mExoPlayer.setTextOutput(this)
        mTransportControls.sendCustomAction(ACTION_SET_REPEAT, bundle(KEY_REPEAT, VAL_REPEAT_OFF))
    }

    fun onMediaRef(mediaRef: MediaRef, playbackExtras: PlaybackExtras) {
        mTransportControls.playFromMediaId(mediaRef.toJson(), playbackExtras.bundle())

        val intent = Intent(mContext, PlaybackActivity::class.java)
                .setAction(ACTION_RESUME).putExtra(EXTRA_MEDIAID, mediaRef.toJson())
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

/**
 *
 */
class PlaybackActivity: BaseVideoActivity(), PlaybackActionsHandler {

    lateinit var mBinding: ActivityPlaybackBinding
    lateinit var mViewModel: PlaybackViewModel
    val mMainHandler = Handler(Looper.getMainLooper())
    val mTotalTimeStringBuilder = StringBuilder()
    val mCurrentTimeStringBuilder = StringBuilder()
    var mPlaybackState = PlaybackState.STATE_NONE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.d("onCreate()")

        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_playback)
        mBinding.actionHandler = this

        mViewModel = fetchViewModel(PlaybackViewModel::class)

        mViewModel.systemTime.observe(this, LiveDataObserver { time ->
            mBinding.systemTimeString = time
        })
        mViewModel.videoDescription.observe(this, LiveDataObserver { desc ->
            mBinding.desc = desc
        })
        mViewModel.playbackTotalTimeSeconds.observe(this, LiveDataObserver { duration ->
            formatTime(duration, mTotalTimeStringBuilder)
            mBinding.totalTimeString = mTotalTimeStringBuilder.toString()
        })
        mViewModel.playbackCurrentTimeSeconds.observe(this, LiveDataObserver { current ->
            formatTime(current, mCurrentTimeStringBuilder)
            mBinding.currentTimeString = mCurrentTimeStringBuilder.toString()
        })
        mViewModel.playbackCurrentProgress.observe(this, LiveDataObserver { current ->
            mBinding.progressVal = current
        })
        mViewModel.playbackState.observe(this, LiveDataObserver { state ->
            mPlaybackState = state.state
            when (state.state) {
                PlaybackState.STATE_PLAYING -> {
                    postOverlayHideRunner()
                }
                PlaybackState.STATE_PAUSED -> {
                    animateOverlayIn()
                }
                PlaybackState.STATE_STOPPED -> {
                    finish()
                }
            }
        })
        mViewModel.captions.observe(this, LiveDataObserver { cues ->
            mBinding.subtitles.setCues(cues)
        })
        mViewModel.aspectRatio.observe(this, LiveDataObserver { ratio ->
            mBinding.surfaceContainer.setAspectRatio(ratio)
        })

        mViewModel.attachSurface(mBinding.videoSurface)

        handleIntent(this.intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        Timber.d("onNewIntent()")
        if (intent != null) {
            handleIntent(intent)
        }
    }

    private fun handleIntent(intent: Intent) {
        val mediaRef = newMediaRef(intent.getStringExtra(EXTRA_MEDIAID))
        val playbackExtras = PlaybackExtras()
        playbackExtras.playWhenReady = intent.getBooleanExtra(EXTRA_PLAY_WHEN_READY, true)
        playbackExtras.resume = intent.action == ACTION_RESUME
        mViewModel.onMediaRef(mediaRef, playbackExtras)
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.d("onDestroy()")
        mBinding.actionHandler = null
        mViewModel.detachSurface(mBinding.videoSurface)
        mMainHandler.removeCallbacksAndMessages(null)
    }

    override fun onStart() {
        Timber.d("onStart()")
        super.onStart()
    }

    override fun onStop() {
        super.onStop()
        Timber.d("onStop()")
        mViewModel.pausePlayback()
    }

    override fun onResume() {
        super.onResume()
        Timber.d("onResume()")
        animateOverlayIn()
    }

    override fun onPause() {
        super.onPause()
        Timber.d("onPause()")
        if (!isInPictureInPictureMode) {
            if (mPlaybackState == PlaybackState.STATE_PLAYING) {
                if (!requestVisibleBehind(true)) {
                    mViewModel.pausePlayback()
                }
            }
        }
    }

    override fun onVisibleBehindCanceled() {
        super.onVisibleBehindCanceled()
        Timber.d("onVisibleBehindCanceled()")
        mViewModel.pausePlayback()
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode)
        if (isInPictureInPictureMode) {
            animateOverlayOut()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        Timber.d("onKeyDown(%s)", KeyEvent::class.java.declaredFields.filter {
            it.name.startsWith("KEYCODE") && it.getInt(null) == keyCode
        }.firstOrNull()?.name)

        if (isOverlayShowing()) {
            postOverlayHideRunner()
            return super.onKeyDown(keyCode, event)
        } else {
            when (keyCode) {
                KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_DPAD_DOWN -> {
                    animateOverlayIn()
                }
                KeyEvent.KEYCODE_DPAD_RIGHT -> {
                    mViewModel.skipAhead()
                }
                KeyEvent.KEYCODE_DPAD_LEFT -> {
                    mViewModel.skipBehind()
                }
                else -> {
                    return super.onKeyDown(keyCode, event)
                }
            }
            return true
        }
    }

    override fun onBackPressed() {
        if (isOverlayShowing()) {
            animateOverlayOut()
        } else {
            super.onBackPressed()
        }
    }

    /*
     * start playback actions handler
     */

    override fun toggleCaptions() {
        if (mBinding.subtitles.visibility != View.VISIBLE) {
            mBinding.subtitles.visibility = View.VISIBLE
        } else {
            mBinding.subtitles.visibility = View.INVISIBLE
        }
    }

    override fun togglePlayPause() {
        mViewModel.togglePlayPause()
    }

    override fun skipPrevious() {
        mViewModel.skipPrevious()
    }

    override fun skipNext() {
        mViewModel.skipNext()
    }

    override fun enterPip() {
        enterPictureInPictureMode()
    }

    /*
     * end playback actions handler
     */

    fun animateOverlayIn() {
        mBinding.topBar.animate().cancel()
        if (mBinding.topBar.visibility != View.VISIBLE) {
            mBinding.topBar.translationY = -mBinding.topBar.height.toFloat()
            mBinding.topBar.visibility = View.VISIBLE
        }
        if (mBinding.topBar.alpha != 1.0f) {
            mBinding.topBar.alpha = 1.0f
        }
        if (mBinding.topBar.translationY != 0.0f) {
            mBinding.topBar.animate()
                    .setDuration(OVERLAY_ANIM_DURATION)
                    .translationY(0.0f)
                    .setListener(null)
        }

        mBinding.bottomBar.animate().cancel()
        if (mBinding.bottomBar.visibility != View.VISIBLE) {
            mBinding.bottomBar.translationY = mBinding.bottomBar.height.toFloat()
            mBinding.bottomBar.visibility = View.VISIBLE
        }
        if (mBinding.bottomBar.alpha != 1.0f) {
            mBinding.bottomBar.alpha = 1.0f
        }
        if (mBinding.bottomBar.translationY != 0.0f) {
            mBinding.bottomBar.animate()
                    .setDuration(OVERLAY_ANIM_DURATION)
                    .translationY(0.0f)
                    .setListener(null)
        }
        mBinding.actionPlayPause.requestFocus()

        postOverlayHideRunner()
        mViewModel.startProgressRunner()
    }

    fun animateOverlayOut() {
        mMainHandler.removeCallbacks(mAnimateOutRunner)
        mViewModel.stopProgressRunner()

        if (mBinding.topBar.visibility != View.GONE) {
            mBinding.topBar.animate().cancel()
            mBinding.topBar.animate()
                    .translationY(-mBinding.topBar.height.toFloat())
                    .alpha(0.0f)
                    .setDuration(OVERLAY_ANIM_DURATION)
                    .setListener(object: DefaultAnimatorListener() {
                        override fun onAnimationEnd(animation: Animator?) {
                            mBinding.topBar.visibility = View.GONE
                        }
                    })
        }
        if (mBinding.bottomBar.visibility != View.GONE) {
            mBinding.bottomBar.animate().cancel()
            mBinding.bottomBar.animate()
                    .translationY(mBinding.bottomBar.height.toFloat())
                    .alpha(0.0f)
                    .setDuration(OVERLAY_ANIM_DURATION)
                    .setListener(object: DefaultAnimatorListener() {
                        override fun onAnimationEnd(animation: Animator?) {
                            mBinding.bottomBar.visibility = View.GONE
                        }
                    })
        }
    }

    private val mAnimateOutRunner = Runnable { animateOverlayOut() }

    fun postOverlayHideRunner() {
        mMainHandler.removeCallbacks(mAnimateOutRunner)
        mMainHandler.postDelayed(mAnimateOutRunner, OVERLAY_SHOW_DURATION)
    }

    fun isOverlayShowing(): Boolean {
        return (mBinding.bottomBar.visibility != View.GONE) ||
                (mBinding.topBar.visibility != View.GONE)
    }

}

open class DefaultAnimatorListener: Animator.AnimatorListener {
    override fun onAnimationRepeat(animation: Animator?) {
    }

    override fun onAnimationEnd(animation: Animator?) {
    }

    override fun onAnimationCancel(animation: Animator?) {
    }

    override fun onAnimationStart(animation: Animator?) {
    }
}
