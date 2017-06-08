package org.opensilk.video.telly

import android.databinding.DataBindingUtil
import android.media.browse.MediaBrowser
import android.os.Bundle
import dagger.BindsInstance
import dagger.Module
import dagger.Subcomponent
import org.opensilk.common.dagger.ActivityScope
import org.opensilk.common.dagger.Injector
import org.opensilk.video.telly.databinding.ActivityPlaybackBinding

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_playback)
    }

}
