package org.opensilk.media

import android.media.MediaDescription
import android.media.browse.MediaBrowser
import android.net.Uri
import android.os.Build
import timber.log.Timber

/**
 * Created by drew on 6/26/16.
 */
/*
 * Extensions for MediaItem and MediaDescription
 */

fun MediaDescription.Builder._setMediaUri(metaExtras: MediaMeta, uri: Uri): MediaDescription.Builder {
    metaExtras.mediaUri = uri
    if (Build.VERSION.SDK_INT >= 23) {
        this.setMediaUri(uri)
    }
    return this
}

fun MediaBrowser.MediaItem._getMediaUri(): Uri {
    return this.description._getMediaUri()
}

fun MediaDescription._getMediaUri(): Uri {
    return if (Build.VERSION.SDK_INT >= 23) {
        this.mediaUri
    } else {
        val metaExtras = MediaMeta.from(this)
        metaExtras.mediaUri
    }
}

fun MediaBrowser.MediaItem._getMediaTitle(): String {
    return this.description._getMediaTitle()
}

fun MediaDescription._getMediaTitle(): String {
    val metaExtras = MediaMeta.from(this)
    var mediaTitle = metaExtras.displayName
    if (mediaTitle == "") {
        mediaTitle = title?.toString() ?: ""
    }
    return mediaTitle
}

fun MediaDescription._newBuilder(): MediaDescription.Builder {
    val bob = MediaDescription.Builder()
            .setIconUri(this.iconUri)
            .setMediaId(this.mediaId)
            .setExtras(this.extras)
            .setSubtitle(this.subtitle)
            .setTitle(this.title)
            .setDescription(this.description)
    if (Build.VERSION.SDK_INT >= 23) {
        bob.setMediaUri(this.mediaUri)
    }
    return bob
}

fun MediaBrowser.MediaItem._copy(bob: MediaDescription.Builder, meta: MediaMeta): MediaBrowser.MediaItem {
    return MediaBrowser.MediaItem(bob._setMediaMeta(meta).build(), this.flags)
}

fun newMediaItem(bob: MediaDescription.Builder, meta: MediaMeta): MediaBrowser.MediaItem {
    return MediaBrowser.MediaItem(bob.setExtras(meta.meta).build(), meta.mediaItemFlags)
}
