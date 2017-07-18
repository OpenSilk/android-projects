package org.opensilk.video.telly

import android.media.browse.MediaBrowser
import dagger.BindsInstance
import dagger.Module
import dagger.Subcomponent
import org.opensilk.common.dagger.ActivityScope
import org.opensilk.common.dagger.Injector

/**
 * Created by drew on 6/7/17.
 */
@ActivityScope
@Subcomponent
interface MockPlaybackComponent: Injector<PlaybackActivity> {
    @Subcomponent.Builder
    abstract class Builder: Injector.Builder<PlaybackActivity>()
}

/**
 *
 */
@Module(subcomponents = arrayOf(MockPlaybackComponent::class))
abstract class MockPlaybackModule