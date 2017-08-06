package org.opensilk.media

import android.media.browse.MediaBrowser
import android.net.Uri
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.Single

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