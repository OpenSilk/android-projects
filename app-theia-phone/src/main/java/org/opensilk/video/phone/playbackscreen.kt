package org.opensilk.video.phone

import android.animation.Animator
import android.content.ComponentName
import android.content.Intent
import android.content.res.Configuration
import android.databinding.DataBindingUtil
import android.media.session.PlaybackState
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.*
import android.widget.Toast
import dagger.Module
import dagger.Subcomponent
import dagger.android.ContributesAndroidInjector
import org.opensilk.common.dagger.Injector
import org.opensilk.media.parseMediaId
import org.opensilk.media.playback.PlaybackExtras
import org.opensilk.video.*
import org.opensilk.video.phone.databinding.ActivityPlaybackBinding
import timber.log.Timber

interface PlaybackActionsHandler {
    fun toggleCaptions()
    fun togglePlayPause()
    fun skipPrevious()
    fun skipNext()
    fun enterPip()
}

/**
 * Created by drew on 8/9/17.
 */
class PlaybackActivity: BaseVideoActivity(), PlaybackActionsHandler,
        GestureDetector.OnGestureListener, GestureDetector.OnDoubleTapListener {

    lateinit var mBinding: ActivityPlaybackBinding
    lateinit var mViewModel: PlaybackViewModel
    val mMainHandler = Handler(Looper.getMainLooper())
    val mTotalTimeStringBuilder = StringBuilder()
    val mCurrentTimeStringBuilder = StringBuilder()
    var mPlaybackState = PlaybackState.STATE_NONE
    lateinit var mGestureDetector: GestureDetector

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.d("onCreate()")

        //npe if init-ed in declaration
        mGestureDetector = GestureDetector(this, this)
        mGestureDetector.setOnDoubleTapListener(this)

        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_playback)
        mBinding.actionHandler = this

        mBinding.root.setOnSystemUiVisibilityChangeListener { visibility ->
            if ((visibility and View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                animateOverlayIn()
            } else if (isOverlayShowing()) {
                animateOverlayOut()
            }
        }

        mViewModel = fetchViewModel(PlaybackViewModel::class)

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
            mBinding.isPlaying = state.state == PlaybackState.STATE_PLAYING
            when (state.state) {
                PlaybackState.STATE_PLAYING -> {
                    postOverlayHideRunner()
                    mBinding.root.keepScreenOn = true
                }
                PlaybackState.STATE_PAUSED -> {
                    animateOverlayIn()
                    mBinding.root.keepScreenOn = false
                }
                PlaybackState.STATE_STOPPED -> {
                    finish()
                }
                PlaybackState.STATE_ERROR -> {
                    mBinding.errorMsg = state.errorMessage?.toString()
                            ?: getString(R.string.err_unknown)
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
        val mediaRef = parseMediaId(intent.getStringExtra(EXTRA_MEDIAID))
        val playbackExtras = PlaybackExtras()
        playbackExtras.playWhenReady = intent.getBooleanExtra(EXTRA_PLAY_WHEN_READY, true)
        playbackExtras.resume = intent.action == ACTION_RESUME
        mViewModel.onMediaRef(mediaRef, playbackExtras,
                ComponentName(this, PlaybackActivity::class.java))
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
        if (Build.VERSION.SDK_INT >= 24) {
            mViewModel.pausePlayback()
        }
    }

    override fun onResume() {
        super.onResume()
        Timber.d("onResume()")
        animateOverlayIn()
    }

    override fun onPause() {
        super.onPause()
        Timber.d("onPause()")
        if (Build.VERSION.SDK_INT < 24) {
            mViewModel.pausePlayback()
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

    override fun onBackPressed() {
        if (isOverlayShowing()) {
            animateOverlayOut()
        } else {
            super.onBackPressed()
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return mGestureDetector.onTouchEvent(event) || super.onTouchEvent(event)
    }

    override fun onShowPress(e: MotionEvent) {
        Timber.d("onShowPress()")
    }

    override fun onSingleTapUp(e: MotionEvent): Boolean {
        Timber.d("onSingleTapUp()")
        return false
    }

    override fun onDown(e: MotionEvent): Boolean {
        Timber.d("onDown()")
        return false
    }

    override fun onFling(e1: MotionEvent, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
        Timber.d("onFling(vx=$velocityX, vy=$velocityY")
        return false
    }

    override fun onScroll(e1: MotionEvent?, e2: MotionEvent?, distanceX: Float, distanceY: Float): Boolean {
        Timber.d("onScroll(dx=$distanceX, dy=$distanceY)")
        return false
    }

    override fun onLongPress(e: MotionEvent) {
        Timber.d("onLongPress()")
    }

    override fun onDoubleTap(e: MotionEvent?): Boolean {
        Timber.d("onDoubleTap()")
        animateOverlayIn()
        if (mPlaybackState == PlaybackState.STATE_PLAYING) {
            animateOverlayIn()
        } else {
            animateOverlayOut()
        }
        mViewModel.togglePlayPause()
        return false
    }

    override fun onDoubleTapEvent(e: MotionEvent?): Boolean {
        Timber.d("onDoubleTapEvent()")
        return false
    }

    override fun onSingleTapConfirmed(e: MotionEvent?): Boolean {
        Timber.d("onSingleTapConfirmed()")
        animateOverlayIn()
        return true
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
        if (Build.VERSION.SDK_INT >= 24) {
            enterPictureInPictureMode()
        } else {
            Toast.makeText(this, R.string.err_pip_unsupported, Toast.LENGTH_SHORT).show()
        }
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

        mBinding.root.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE

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

        mBinding.root.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LOW_PROFILE
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