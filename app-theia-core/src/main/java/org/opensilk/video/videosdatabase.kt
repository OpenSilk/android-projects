package org.opensilk.video

import android.media.browse.MediaBrowser
import dagger.Binds
import dagger.Module
import org.opensilk.media.MediaRef
import org.opensilk.media.playback.MediaProviderClient
import rx.Single
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Created by drew on 6/15/17.
 */

@Module
abstract class ProviderModule {
    @Binds abstract fun providerClient(providerClient: VideosProviderClient): MediaProviderClient
}

@Singleton
class VideosProviderClient
@Inject
constructor(

) : MediaProviderClient {
    override fun getMediaItem(mediaRef: MediaRef): Single<MediaBrowser.MediaItem> {
        TODO("not implemented")
    }
}
