package org.opensilk.video.telly

import android.media.browse.MediaBrowser
import dagger.BindsInstance
import dagger.Component
import dagger.Module
import dagger.Subcomponent
import org.opensilk.common.dagger.Injector

/**
 * Created by drew on 6/2/17.
 */
@Subcomponent()
interface MockDetailComponent: Injector<DetailFragment> {
    @Component.Builder
    abstract class Builder: Injector.Builder<DetailFragment>() {
        @BindsInstance
        abstract fun mediaItem(mediaItem: MediaBrowser.MediaItem): Builder
    }
}

/**
 *
 */
@Module(subcomponents = arrayOf(MockDetailComponent::class))
abstract class MockDetailModule