package org.opensilk.video.telly

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
import android.view.KeyEvent
import android.view.View
import android.widget.Toast
import org.opensilk.media.getMediaIdExtra
import org.opensilk.media.playback.PlaybackExtras
import org.opensilk.media.toMediaId
import org.opensilk.video.*
import org.opensilk.video.telly.databinding.ActivityPlaybackBinding
import timber.log.Timber

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
            mBinding.isPlaying = state.state == PlaybackState.STATE_PLAYING
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
                PlaybackState.STATE_ERROR -> {
                    mBinding.errorMsg = state.errorMessage?.toString()
                            ?: getString(R.string.unknown_error)
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
        val mediaRef = intent.getMediaIdExtra()
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
        if (Build.VERSION.SDK_INT < 24 || !isInPictureInPictureMode) {
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

        when (keyCode) {
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                mViewModel.togglePlayPause()
                return true
            }
        }

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
        if (Build.VERSION.SDK_INT >= 24) {
            enterPictureInPictureMode()
        } else {
            Toast.makeText(this, R.string.err_pip_unsupported, Toast.LENGTH_LONG).show()
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
