package org.opensilk.video.telly

import android.media.browse.MediaBrowser
import dagger.BindsInstance
import dagger.Component
import dagger.Module
import dagger.Subcomponent
import org.opensilk.common.dagger.ActivityScope
import org.opensilk.common.dagger.Injector

/**
 * Created by drew on 6/2/17.
 */
@ActivityScope
@Subcomponent(modules = arrayOf(DetailPresenterModule::class))
interface MockDetailComponent: Injector<DetailFragment> {
    @Subcomponent.Builder
    abstract class Builder: Injector.Builder<DetailFragment>() {
        override fun create(t: DetailFragment): Injector<DetailFragment> {
            val mediaItem: MediaBrowser.MediaItem = t.activity.intent.getParcelableExtra(EXTRA_MEDIAITEM)
            return mediaItem(mediaItem).build()
        }
        @BindsInstance
        abstract fun mediaItem(mediaItem: MediaBrowser.MediaItem): Builder
    }
}

/**
 *
 */
@Module(subcomponents = arrayOf(MockDetailComponent::class))
abstract class MockDetailModule