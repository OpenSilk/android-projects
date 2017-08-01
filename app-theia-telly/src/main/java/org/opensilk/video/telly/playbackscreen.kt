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
import android.view.View
import android.widget.Toast
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.text.Cue
import com.google.android.exoplayer2.text.TextRenderer
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import dagger.Binds
import dagger.BindsInstance
import dagger.Module
import dagger.Subcomponent
import dagger.multibindings.IntoMap
import org.opensilk.common.dagger.ActivityScope
import org.opensilk.common.dagger.ForApplication
import org.opensilk.common.dagger.Injector
import org.opensilk.common.dagger.injectMe
import org.opensilk.media.*
import org.opensilk.media.playback.*
import org.opensilk.video.*
import org.opensilk.video.telly.databinding.ActivityPlaybackBinding
import rx.Observable
import rx.Scheduler
import rx.android.schedulers.AndroidSchedulers
import rx.subscriptions.Subscriptions
import timber.log.Timber
import java.util.*
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
        private val mClient: MediaProviderClient
) : ViewModel(), LifecycleObserver {
    val videoDescription = MutableLiveData<VideoDescInfo>()

    fun onMediaRef(mediaRef: MediaRef) {
        mClient.getMediaMeta(mediaRef)
                .subscribeOn(AppSchedulers.diskIo)
                .subscribe({
                    videoDescription.postValue(VideoDescInfo(
                            it.title.elseIfBlank(it.displayName),
                            it.subtitle,
                            ""
                    ))
                }, {
                    Timber.e(it, "")
                })
    }

    val currentTime = MutableLiveData<String>()

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun registerTimeTick() {
        currentTime.value = DateFormat.getTimeFormat(mContext).format(Date())
        mContext.registerReceiver(timetick, IntentFilter(Intent.ACTION_TIME_TICK))
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun unregisterTimeTick() {
        mContext.unregisterReceiver(timetick)
    }

    private val timetick = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            currentTime.value = DateFormat.getTimeFormat(context).format(Date())
        }
    }
}

/**
 *
 */
class PlaybackActivity: BaseVideoActivity(), PlaybackActionsHandler,
        MediaBrowserCallback.Listener, MediaControllerCallback.Listener,
        SimpleExoPlayer.VideoListener, TextRenderer.Output {

    lateinit var mBinding: ActivityPlaybackBinding
    lateinit var mBrowser: MediaBrowser
    lateinit var mExoPlayer: SimpleExoPlayer
    lateinit var mMediaRef: MediaRef
    lateinit var mViewModel: PlaybackViewModel
    var mPlayWhenReady: Boolean = true
    var mResumePlayback: Boolean = false
    val mMainHandler = Handler(Looper.getMainLooper())
    val mMediaControllerCallback = MediaControllerCallback(this)
    var mMediaControllerCallbackRegistered = false
    var mWonVisibleBehind = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.d("onCreate()")

        mMediaRef = newMediaRef(intent.getStringExtra(EXTRA_MEDIAID))
        mPlayWhenReady = intent.getBooleanExtra(EXTRA_PLAY_WHEN_READY, true)
        mResumePlayback = intent.action == ACTION_RESUME

        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_playback)
        mBinding.actionHandler = this

        mViewModel = fetchViewModel(PlaybackViewModel::class)
        mViewModel.videoDescription.observe(this, LiveDataObserver {
            mBinding.desc = it
        })
        mViewModel.currentTime.observe(this, LiveDataObserver {
            mBinding.systemTimeString = it
        })
        mViewModel.onMediaRef(mMediaRef)

        mBrowser = MediaBrowser(this, ComponentName(this, PlaybackService::class.java),
                MediaBrowserCallback(this), null)
        mBrowser.connect()
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.d("onDestroy()")
        mBinding.actionHandler = null
        mMainHandler.removeCallbacksAndMessages(null)
        cleanupSessionStuffs()
        mBrowser.disconnect()
    }

    override fun onStart() {
        Timber.d("onStart()")
        super.onStart()
    }

    override fun onStop() {
        super.onStop()
        Timber.d("onStop()")
        if (isFinishing || !mWonVisibleBehind) {
            if (mBrowser.isConnected) mediaController.transportControls.pause()
        }
    }

    override fun onResume() {
        super.onResume()
        Timber.d("onResume()")
        if (mBrowser.isConnected &&
                mediaController.playbackState.state == PlaybackState.STATE_PAUSED) {
            //make sure overlay is showing
            mBinding.topBar.visibility = View.INVISIBLE
            mBinding.bottomBar.visibility = View.INVISIBLE
            //select playpause
            mBinding.actionPlayPause.requestFocus()
            animateOverlayIn()
        } else {
            mBinding.topBar.visibility = View.GONE
            mBinding.bottomBar.visibility = View.GONE
        }
    }

    override fun onPause() {
        super.onPause()
        Timber.d("onPause()")
        mWonVisibleBehind = if (mBrowser.isConnected &&
                mediaController.playbackState.state != PlaybackState.STATE_PAUSED &&
                !isInPictureInPictureMode &&
                !isFinishing) requestVisibleBehind(true) else false
    }

    override fun onVisibleBehindCanceled() {
        super.onVisibleBehindCanceled()
        Timber.d("onVisibleBehindCanceled()")
        if (mBrowser.isConnected) mediaController.transportControls.pause()
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
                    skipAhead()
                }
                KeyEvent.KEYCODE_DPAD_LEFT -> {
                    skipBehind()
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

    /**
     * Seeks ahead by SKIP_AHEAD_MS
     */
    fun skipAhead() {
        if (mBrowser.notConnected()) return
        if (mediaController.playbackState.hasAction(PlaybackState.ACTION_SEEK_TO)) {
            val offset = SystemClock.elapsedRealtime() - mediaController.playbackState.lastPositionUpdateTime
            val seek = mediaController.playbackState.position + offset + SKIP_AHEAD_MS
            mediaController.transportControls.seekTo(seek)
        }
    }

    /**
     * Seeks back by SKIP_BEHIND_MS
     */
    fun skipBehind() {
        if (mBrowser.notConnected()) return
        if (mediaController.playbackState.hasAction(PlaybackState.ACTION_SEEK_TO)) {
            val offset = SystemClock.elapsedRealtime() - mediaController.playbackState.lastPositionUpdateTime
            val seek = mediaController.playbackState.position + offset - SKIP_BEHIND_MS
            mediaController.transportControls.seekTo(seek)
        }
    }

    /*
     * start browser callback
     */

    override fun onBrowserConnected() {
        mediaController = MediaController(this, mBrowser.sessionToken)
        mediaController.registerCallback(mMediaControllerCallback, mMainHandler)
        mMediaControllerCallbackRegistered = true
        mediaController.sendCommand(CMD_GET_EXOPLAYER, bundle(), object : ResultReceiver(null) {
            override fun onReceiveResult(resultCode: Int, resultData: Bundle?) {
                when (resultCode) {
                    CMD_RESULT_OK -> {
                        val binder = resultData!!.getBinder(CMD_RESULT_ARG1) as PlaybackSession.SessionBinder
                        mExoPlayer = binder.player
                        attachExoPlayer()
                        val extras = PlaybackExtras()
                        extras.playWhenReady = mPlayWhenReady
                        extras.resume = mResumePlayback
                        mediaController.transportControls.playFromMediaId(mMediaRef.toJson(), extras.bundle())
                    }
                    else -> {
                        throw RuntimeException("Invalid return value on CMD_GET_EXOPLAYER val=$resultCode")
                    }
                }
            }
        })
    }

    override fun onBrowserDisconnected() {
        cleanupSessionStuffs()
    }

    /*
     * end browser callback
     */

    /*
     * start playback actions handler
     */

    override fun toggleCaptions() {
        if (mBrowser.notConnected()) return

        if (mBinding.subtitles.visibility == View.VISIBLE) {
            mBinding.subtitles.visibility = View.INVISIBLE
        } else {
            mBinding.subtitles.visibility = View.VISIBLE
        }
    }

    override fun togglePlayPause() {
        if (mBrowser.notConnected()) return

        val pbs = mediaController.playbackState
        if (pbs.hasAction(PlaybackState.ACTION_PLAY)) {
            mediaController.transportControls.play()
        } else {
            mediaController.transportControls.pause()
        }
    }

    override fun skipPrevious() {
        if (mBrowser.notConnected()) return

        val pbs = mediaController.playbackState
        if (pbs.hasAction(PlaybackState.ACTION_SKIP_TO_PREVIOUS)) {
            mediaController.transportControls.skipToPrevious()
        }
    }

    override fun skipNext() {
        if (mBrowser.notConnected()) return

        val pbs = mediaController.playbackState
        if (pbs.hasAction(PlaybackState.ACTION_SKIP_TO_NEXT)) {
            mediaController.transportControls.skipToNext()
        }
    }

    override fun enterPip() {
        enterPictureInPictureMode()
    }

    /*
     * end playback actions handler
     */

    /*
     * start media session callback
     */

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
        when (state.state) {
            PlaybackState.STATE_PLAYING -> {
                animateOverlayOut()
            }
            PlaybackState.STATE_PAUSED -> {
                animateOverlayIn()
            }
            PlaybackState.STATE_STOPPED -> {
                finish()
            }
        }
    }

    override fun onMetadataChanged(metadata: MediaMetadata) {
        mBinding.desc = VideoDescInfo(
                metadata.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE) ?: "",
                metadata.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE) ?: "",
                metadata.getString(MediaMetadata.METADATA_KEY_DISPLAY_DESCRIPTION) ?: ""
        )
    }

    override fun onSessionDestroyed() {
        cleanupSessionStuffs()
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

    override fun onVideoSizeChanged(width: Int, height: Int, unappliedRotationDegrees: Int, pixelWidthHeightRatio: Float) {
        val aspectRatio: Float = if (height == 0) 1f else (width.toFloat() * pixelWidthHeightRatio) / height.toFloat()
        mBinding.surfaceContainer.setAspectRatio(aspectRatio)
    }

    override fun onRenderedFirstFrame() {

    }

    override fun onCues(cues: MutableList<Cue>?) {
        mBinding.subtitles.onCues(cues)
    }

    /*
     * end exoplayer callbacks
     */

    private fun cleanupSessionStuffs() {
        detachExoPlayer()
        unregisterMediaControllerCallback()
        mediaController = null
    }

    private fun attachExoPlayer() {
        mExoPlayer.setVideoSurfaceView(mBinding.videoSurface)
        mExoPlayer.setVideoListener(this)
        mExoPlayer.setTextOutput(this)
    }

    private fun detachExoPlayer() {
        mExoPlayer.clearVideoSurfaceView(mBinding.videoSurface)
        mExoPlayer.clearVideoListener(this)
        mExoPlayer.clearTextOutput(this)
    }

    private fun unregisterMediaControllerCallback() {
        if (mMediaControllerCallbackRegistered) {
            mediaController?.let {
                it.unregisterCallback(mMediaControllerCallback)
                mMediaControllerCallbackRegistered = false
            }
        }
    }

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
            mBinding.actionPlayPause.requestFocus()
        }

        postOverlayHideRunner()
    }

    fun animateOverlayOut() {
        mMainHandler.removeCallbacks(mAnimateOutRunner)

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
