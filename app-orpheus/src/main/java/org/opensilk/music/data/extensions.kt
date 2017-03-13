package org.opensilk.music.data

import android.media.MediaDescription
import android.media.browse.MediaBrowser
import android.net.Uri
import org.opensilk.common.isAtLeastApi23
import org.opensilk.media.MediaMeta
import org.opensilk.media._getMediaUri
import org.opensilk.music.data.ref.DocumentRef
import org.opensilk.music.data.ref.MEDIA_KIND_DOCUMENT
import org.opensilk.music.data.ref.MediaRef
import timber.log.Timber

fun MediaBrowser.MediaItem._getMediaRef(): MediaRef {
    return MediaRef.parse(this.mediaId)
}

fun MediaBrowser.MediaItem._likelyDocument(): Boolean {
    return MediaRef.extractKind(this.mediaId) == MEDIA_KIND_DOCUMENT
}