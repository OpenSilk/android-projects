package org.opensilk.media.playback

import android.net.Uri
import io.reactivex.Maybe
import io.reactivex.Observable
import org.opensilk.media.MediaId
import org.opensilk.media.MediaRef

/**
 * Created by drew on 6/26/17.
 */
interface MediaProviderClient {
    fun getMediaMeta(mediaId: MediaId): Maybe<out MediaRef>
    fun getMediaArtworkUri(mediaId: MediaId): Maybe<Uri>
    fun siblingsOf(mediaId: MediaId): Observable<out MediaRef>
    fun getLastPlaybackPosition(mediaId: MediaId): Maybe<Long>
    fun setLastPlaybackPosition(mediaId: MediaId, position: Long, duration: Long)
}