package org.opensilk.video.telly

import android.animation.Animator
import android.content.*
import android.databinding.DataBindingUtil
import android.media.MediaMetadata
import android.media.browse.MediaBrowser
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.ResultReceiver
import android.text.format.DateFormat
import android.view.View
import android.widget.Toast
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.text.Cue
import com.google.android.exoplayer2.text.TextRenderer
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import dagger.BindsInstance
import dagger.Module
import dagger.Subcomponent
import org.opensilk.common.dagger.ActivityScope
import org.opensilk.common.dagger.Injector
import org.opensilk.common.dagger.injectMe
import org.opensilk.media.MediaBrowserCallback
import org.opensilk.media.MediaControllerCallback
import org.opensilk.media.bundle
import org.opensilk.media.notConnected
import org.opensilk.media.playback.*
import org.opensilk.video.PlaybackService
import org.opensilk.video.VideoDescInfo
import org.opensilk.video.telly.databinding.ActivityPlaybackBinding
import org.opensilk.video.videoDescInfo
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

/**
 * Created by drew on 6/3/17.
 */
@ActivityScope
@Subcomponent
interface PlaybackComponent: Injector<PlaybackActivity> {
    @Subcomponent.Builder
    abstract class Builder: Injector.Builder<PlaybackActivity>() {
        override fun create(t: PlaybackActivity): Injector<PlaybackActivity> {
            val mediaItem: MediaBrowser.MediaItem = t.intent.getParcelableExtra(EXTRA_MEDIAITEM)
            return mediaItem(mediaItem).build()
        }
        @BindsInstance
        abstract fun mediaItem(mediaItem: MediaBrowser.MediaItem): Builder
    }
}

/**
 *
 */
@Module(subcomponents = arrayOf(PlaybackComponent::class))
abstract class PlaybackModule

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
class PlaybackActivity: BaseVideoActivity(), PlaybackActionsHandler,
        MediaBrowserCallback.Listener, MediaControllerCallback.Listener,
        SimpleExoPlayer.VideoListener, TextRenderer.Output {

    lateinit var mMainWorker: Scheduler.Worker
    lateinit var mBinding: ActivityPlaybackBinding
    @Inject lateinit var mMediaItem: MediaBrowser.MediaItem
    lateinit var mBrowser: MediaBrowser
    lateinit var mExoPlayer: SimpleExoPlayer
    val mMainHandler = Handler(Looper.getMainLooper())
    val mMediaControllerCallback = MediaControllerCallback(this)
    var mMediaControllerCallbackRegistered = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mMainWorker = AndroidSchedulers.mainThread().createWorker()
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_playback)
        mBinding.desc = mMediaItem.description.videoDescInfo()
        mBinding.actionHandler = this

        mBrowser = MediaBrowser(this, ComponentName(this, PlaybackService::class.java),
                MediaBrowserCallback(this), null)
        mBrowser.connect()
    }

    override fun onDestroy() {
        super.onDestroy()
        mBinding.actionHandler = null
        mMainWorker.unsubscribe()
        cleanupSessionStuffs()
        mBrowser.disconnect()
    }

    override fun onStart() {
        super.onStart()
        setupCurrentTimeText()
    }

    override fun onStop() {
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        //select playpause
        mBinding.actionPlayPause.requestFocus()
        //make sure overlay is showing
        mBinding.topBar.visibility = View.INVISIBLE
        mBinding.bottomBar.visibility = View.INVISIBLE
        animateOverlayIn()
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
                        if (intent.hasExtra(EXTRA_PLAY_WHEN_READY)) {
                            extras.playWhenReady = intent.getBooleanExtra(EXTRA_PLAY_WHEN_READY, true)
                        }
                        if (intent.action == ACTION_RESUME) {
                            extras.resume = true
                        }
                        mediaController.transportControls.playFromMediaId(mMediaItem.mediaId, extras.bundle())
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
        } else if (pbs.hasAction(PlaybackState.ACTION_PAUSE)) {
            mediaController.transportControls.pause()
        } else {
            Toast.makeText(this, "Pause not allowed", Toast.LENGTH_LONG).show()
            Timber.w("Current state does not allow pause state=%s", pbs)
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

    fun setupCurrentTimeText() {
        Observable.create<String> { s ->
            val timetick = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    s.onNext(DateFormat.getTimeFormat(context).format(Date()))
                }
            }
            registerReceiver(timetick, IntentFilter(Intent.ACTION_TIME_TICK))
            //unregister on unsubscribe
            s.add(Subscriptions.create { unregisterReceiver(timetick) })
            //seed initial value
            timetick.onReceive(this, null)
        }.subscribe { time ->
            mBinding.systemTimeString = time
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
    }

    fun animateOverlayOut() {
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
