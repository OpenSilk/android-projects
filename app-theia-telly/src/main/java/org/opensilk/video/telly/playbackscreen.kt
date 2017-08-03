package org.opensilk.video.telly

import android.animation.Animator
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
        @ForApplication private val mContext: Context
) : ViewModel(), LifecycleObserver,
        MediaBrowserCallback.Listener, MediaControllerCallback.Listener,
        SimpleExoPlayer.VideoListener, TextRenderer.Output {

    private lateinit var mMediaRef: MediaRef
    private lateinit var mPlaybackExtras: PlaybackExtras

    fun onMediaRef(mediaRef: MediaRef, playbackExtras: PlaybackExtras) {
        mMediaRef = mediaRef
        mPlaybackExtras = playbackExtras
        loadMediaRef()
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
        if (mMediaControllerCallbackRegistered) {
            mMediaController.unregisterCallback(mMediaControllerCallback)
        }
        mExoPlayer?.clearVideoListener(this)
        mExoPlayer?.clearTextOutput(this)
        mBrowser.disconnect()
        stopProgressRunner()
        mOnAcquireExoPlayer.clear()
    }

    /*
     * start mediasession / browser stuffs
     */

    private var mBrowser: MediaBrowser = MediaBrowser(mContext,
            ComponentName(mContext, PlaybackService::class.java), MediaBrowserCallback(this), null)
    private lateinit var mMediaController: MediaController
    private val mMediaControllerCallback = MediaControllerCallback(this)
    private var mMediaControllerCallbackRegistered = false
    private var mExoPlayer: SimpleExoPlayer? = null
    private val mOnAcquireExoPlayer = ArrayDeque<Consumer<SimpleExoPlayer>>()
    private var mConnectingBrowser = false

    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    fun connectMediaBrowser() {
        if (!mBrowser.isConnected && !mConnectingBrowser) {
            mConnectingBrowser = true
            mBrowser.connect()
        }
    }

    private val mPlaybackState: PlaybackState
        get() = if (isConnected) mMediaController.playbackState else
            PlaybackState.Builder().setState(PlaybackState.STATE_NONE, 0, 1.0f).build()

    private val isConnected: Boolean
        get() = mBrowser.isConnected

    private fun loadMediaRef() {
        if (isConnected) {
            mMediaController.transportControls.playFromMediaId(
                    mMediaRef.toJson(), mPlaybackExtras.bundle())
        }
    }

    fun pausePlayback() {
        if (isConnected) {
            mMediaController.transportControls.pause()
        }
    }

    fun skipAhead() {
        if (mPlaybackState.hasAction(PlaybackState.ACTION_SEEK_TO)) {
            val offset = SystemClock.elapsedRealtime() - mPlaybackState.lastPositionUpdateTime
            val seek = mPlaybackState.position + offset + SKIP_AHEAD_MS
            mMediaController.transportControls.seekTo(seek)
        }
    }

    fun skipBehind() {
        if (mPlaybackState.hasAction(PlaybackState.ACTION_SEEK_TO)) {
            val offset = SystemClock.elapsedRealtime() - mPlaybackState.lastPositionUpdateTime
            val seek = mPlaybackState.position + offset - SKIP_BEHIND_MS
            mMediaController.transportControls.seekTo(seek)
        }
    }

    /**
     * Sets the surface view exoplayer is to use, if we have not acquired the exoplayer yet
     * we add it to the queue, and set it when we acquire it.
     */
    fun attachSurface(view: SurfaceView) {
        mExoPlayer?.setVideoSurfaceView(view) ?: mOnAcquireExoPlayer.add(Consumer {
            it.setVideoSurfaceView(view)
        })
    }

    /**
     * Detach the surface
     */
    fun detachSurface(view: SurfaceView) {
        mExoPlayer?.clearVideoSurfaceView(view)
    }

    /*
     * start browser callback
     */

    override fun onBrowserConnected() {
        mConnectingBrowser = false
        mMediaController = MediaController(mContext, mBrowser.sessionToken)
        mMediaController.registerCallback(mMediaControllerCallback)
        mMediaControllerCallbackRegistered = true
        fetchExoPlayer()
    }

    override fun onBrowserDisconnected() {
        mConnectingBrowser = false
        if (mMediaControllerCallbackRegistered) {
            mMediaController.unregisterCallback(mMediaControllerCallback)
        }
        mExoPlayer?.clearVideoListener(this)
        mExoPlayer?.clearTextOutput(this)
        mExoPlayer?.clearVideoSurface()
        mExoPlayer = null
    }

    /*
     * end browser callback
     */

    private fun fetchExoPlayer() {
        mMediaController.sendCommand(CMD_GET_EXOPLAYER, bundle(), object : ResultReceiver(Handler(Looper.getMainLooper())) {
            override fun onReceiveResult(resultCode: Int, resultData: Bundle?) {
                when (resultCode) {
                    CMD_RESULT_OK -> {
                        val binder = resultData!!.getBinder(CMD_RESULT_ARG1) as PlaybackSession.SessionBinder
                        val player = binder.player
                        player.setVideoListener(this@PlaybackViewModel)
                        player.setTextOutput(this@PlaybackViewModel)
                        while (!mOnAcquireExoPlayer.isEmpty()) {
                            mOnAcquireExoPlayer.poll().accept(player)
                        }
                        mExoPlayer = player
                        loadMediaRef()
                    }
                    else -> {
                        throw RuntimeException("Invalid return value on CMD_GET_EXOPLAYER val=$resultCode")
                    }
                }
            }
        })
    }

    fun togglePlayPause() {
        if (mPlaybackState.hasAction(PlaybackState.ACTION_PLAY)) {
            mMediaController.transportControls.play()
        } else {
            mMediaController.transportControls.pause()
        }
    }

    fun skipPrevious() {
        if (mPlaybackState.hasAction(PlaybackState.ACTION_SKIP_TO_PREVIOUS)) {
            mMediaController.transportControls.skipToPrevious()
        }
    }

    fun skipNext() {
        if (mPlaybackState.hasAction(PlaybackState.ACTION_SKIP_TO_NEXT)) {
            mMediaController.transportControls.skipToNext()
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
        if (mMediaControllerCallbackRegistered) {
            mMediaController.unregisterCallback(mMediaControllerCallback)
        }
        mExoPlayer?.clearVideoListener(this)
        mExoPlayer?.clearTextOutput(this)
        mExoPlayer?.clearVideoSurface()
        mBrowser.disconnect()
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
        val pbs = mPlaybackState
        val current = pbs.position + if (pbs.state == PlaybackState.STATE_PLAYING) {
            (SystemClock.elapsedRealtime() - pbs.lastPositionUpdateTime)
        } else 0L
        val duration = mMediaController.metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L

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
        mViewModel.playbackState.observe(this, LiveDataObserver { state ->
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
            val won = if (mPlaybackState == PlaybackState.STATE_PLAYING && !isFinishing)
                requestVisibleBehind(true) else false
            if (!won) {
                mViewModel.pausePlayback()
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
