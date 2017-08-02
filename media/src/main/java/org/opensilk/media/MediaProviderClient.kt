package org.opensilk.media

import android.media.browse.MediaBrowser
import android.net.Uri
import io.reactivex.Observable
import io.reactivex.Single

/**
 * Created by drew on 6/26/17.
 */
interface MediaProviderClient {
    fun getMediaItem(mediaRef: MediaRef): Single<MediaBrowser.MediaItem>
    fun getMediaMeta(mediaRef: MediaRef): Single<MediaMeta>
    fun getMediaOverview(mediaRef: MediaRef): Single<String>
    fun getMediaArtworkUri(mediaRef: MediaRef): Single<Uri>
    fun siblingsOf(mediaRef: MediaRef): Observable<MediaMeta>
}