package org.opensilk.video

import android.media.browse.MediaBrowser
import android.os.Bundle
import android.service.media.MediaBrowserService
import org.opensilk.common.dagger.Injector
import org.opensilk.common.dagger.injectMe
import org.opensilk.media.bundle
import org.opensilk.media.playback.PlaybackSession
import javax.inject.Inject

@dagger.Subcomponent
interface PlaybackServiceComponent: Injector<PlaybackService> {
    @dagger.Subcomponent.Builder
    abstract class Builder: Injector.Builder<PlaybackService>()
}

@dagger.Module(subcomponents = arrayOf(PlaybackServiceComponent::class))
open class PlaybackServiceModule

/**
 * Created by drew on 6/28/17.
 */
class PlaybackService : MediaBrowserService() {

    @Inject
    lateinit var mPlaybackSession: PlaybackSession

    override fun onCreate() {
        super.onCreate()
        injectMe()
        sessionToken = mPlaybackSession.token
    }

    override fun onLoadChildren(parentId: String?, result: Result<MutableList<MediaBrowser.MediaItem>>?) {
        TODO("not implemented")
    }

    override fun onGetRoot(clientPackageName: String?, clientUid: Int, rootHints: Bundle?): BrowserRoot {
        return BrowserRoot("__ROOT__", bundle())
    }
}