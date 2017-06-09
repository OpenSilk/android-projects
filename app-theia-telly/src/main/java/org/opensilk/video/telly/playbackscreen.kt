package org.opensilk.video.telly

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.databinding.DataBindingUtil
import android.media.browse.MediaBrowser
import android.os.Bundle
import android.text.format.DateFormat
import dagger.BindsInstance
import dagger.Module
import dagger.Subcomponent
import mortar.MortarScope
import org.opensilk.common.dagger.ActivityScope
import org.opensilk.common.dagger.Injector
import org.opensilk.common.dagger.injectMe
import org.opensilk.common.lifecycle.bindToLifeCycle
import org.opensilk.common.lifecycle.lifecycleService
import org.opensilk.common.lifecycle.terminateOnDestroy
import org.opensilk.video.telly.databinding.ActivityPlaybackBinding
import org.opensilk.video.videoDescInfo
import rx.Observable
import rx.subscriptions.Subscriptions
import java.util.*
import javax.inject.Inject

const val ACTION_PLAY = "org.opensilk.action.PLAY"
const val ACTION_RESUME = "org.opensilk.action.RESUME"

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
class PlaybackActivity: BaseVideoActivity() {

    lateinit var mBinding: ActivityPlaybackBinding
    @Inject
    lateinit var mMediaItem: MediaBrowser.MediaItem

    override fun onScopeCreated(scope: MortarScope) {
        super.onScopeCreated(scope)
        injectMe()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_playback)
        mBinding.desc = mMediaItem.description.videoDescInfo()
    }

    override fun onStart() {
        super.onStart()
        setupCurrentTimeText()
    }

    override fun onResume() {
        super.onResume()
        //select playpause
        mBinding.actionPlayPause.requestFocus()
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

}
