package org.opensilk.media.playback

import android.media.browse.MediaBrowser
import org.opensilk.media.MediaRef
import rx.Observable
import rx.Single
import javax.inject.Inject

/**
 * Created by drew on 6/26/17.
 */
interface MediaProviderClient {
    fun getMediaItem(mediaRef: MediaRef): Single<MediaBrowser.MediaItem>
}