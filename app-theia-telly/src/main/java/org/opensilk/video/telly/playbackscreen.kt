package org.opensilk.video.telly

import android.animation.Animator
import android.animation.AnimatorSet
import android.content.*
import android.databinding.DataBindingUtil
import android.media.browse.MediaBrowser
import android.media.session.MediaController
import android.os.Bundle
import android.text.format.DateFormat
import android.view.View
import android.view.ViewAnimationUtils
import android.view.ViewPropertyAnimator
import android.widget.ViewAnimator
import dagger.BindsInstance
import dagger.Module
import dagger.Subcomponent
import mortar.MortarScope
import org.opensilk.common.animation.DefaultAnimatorListener
import org.opensilk.common.dagger.ActivityScope
import org.opensilk.common.dagger.Injector
import org.opensilk.common.dagger.injectMe
import org.opensilk.common.lifecycle.bindToLifeCycle
import org.opensilk.common.lifecycle.lifecycleService
import org.opensilk.common.lifecycle.terminateOnDestroy
import org.opensilk.media.MediaBrowserCallback
import org.opensilk.video.VideoPlaybackService
import org.opensilk.video.telly.databinding.ActivityPlaybackBinding
import org.opensilk.video.videoDescInfo
import rx.Observable
import rx.Scheduler
import rx.android.schedulers.AndroidSchedulers
import rx.subscriptions.Subscriptions
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject

const val ACTION_PLAY = "org.opensilk.action.PLAY"
const val ACTION_RESUME = "org.opensilk.action.RESUME"

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
class PlaybackActivity: BaseVideoActivity(), PlaybackActionsHandler {

    lateinit var mMainWorker: Scheduler.Worker

    lateinit var mBinding: ActivityPlaybackBinding
    @Inject
    lateinit var mMediaItem: MediaBrowser.MediaItem

    lateinit var mBrowser: MediaBrowser

    override fun onScopeCreated(scope: MortarScope) {
        super.onScopeCreated(scope)
        injectMe()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mMainWorker = AndroidSchedulers.mainThread().createWorker()
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_playback)
        mBinding.desc = mMediaItem.description.videoDescInfo()
        mBinding.actionHandler = this

        mBrowser = MediaBrowser(this, ComponentName(this, VideoPlaybackService::class.java), object: MediaBrowser.ConnectionCallback() {
            override fun onConnected() {
                mediaController = MediaController(this@PlaybackActivity, mBrowser.sessionToken)
            }
        }, null)
        mBrowser.connect()
    }

    override fun onDestroy() {
        super.onDestroy()
        mBinding.actionHandler = null
        mMainWorker.unsubscribe()
        mediaController = null
        mBrowser.disconnect()
    }

    override fun onStart() {
        super.onStart()
        setupCurrentTimeText()

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

    override fun toggleCaptions() {

    }

    override fun togglePlayPause() {

    }

    override fun skipPrevious() {

    }

    override fun skipNext() {

    }

    override fun enterPip() {

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
        }.bindToLifeCycle(this).subscribe { time ->
            mBinding.systemTimeString = time
        }
    }

    fun animateOverlayIn() {
        mBinding.topBar.translationY = -mBinding.topBar.height.toFloat()
        mBinding.topBar.alpha = 1.0f
        mBinding.topBar.visibility = View.VISIBLE
        mBinding.topBar.animate().cancel()
        mBinding.topBar.animate()
                .setDuration(OVERLAY_ANIM_DURATION)
                .translationY(0.0f)

        mBinding.bottomBar.translationY = mBinding.bottomBar.height.toFloat()
        mBinding.bottomBar.alpha = 1.0f
        mBinding.bottomBar.visibility = View.VISIBLE
        mBinding.bottomBar.animate().cancel()
        mBinding.bottomBar.animate()
                .setDuration(OVERLAY_ANIM_DURATION)
                .translationY(0.0f)
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

interface PlaybackActionsHandler {
    fun toggleCaptions()
    fun togglePlayPause()
    fun skipPrevious()
    fun skipNext()
    fun enterPip()
}
