package org.opensilk.video.telly

import android.media.browse.MediaBrowser
import dagger.BindsInstance
import dagger.Module
import dagger.Subcomponent
import org.opensilk.common.dagger.ActivityScope
import org.opensilk.common.dagger.Injector

/**
 * Created by drew on 6/1/17.
 */
@ActivityScope
@Subcomponent(modules = arrayOf(MockUpnpLoadersModule::class))
interface MockFolderComponent: Injector<FolderFragment> {
    @Subcomponent.Builder
    abstract class Builder: Injector.Builder<FolderFragment>() {
        override fun create(t: FolderFragment): Injector<FolderFragment> {
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
@Module(subcomponents = arrayOf(MockFolderComponent::class))
abstract class MockFolderModule