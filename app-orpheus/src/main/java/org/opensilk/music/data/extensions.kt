package org.opensilk.music.data

import android.media.MediaDescription
import android.media.browse.MediaBrowser
import android.net.Uri
import org.opensilk.common.isAtLeastApi23
import org.opensilk.music.data.ref.DocumentRef
import org.opensilk.music.data.ref.MEDIA_KIND_DOCUMENT
import org.opensilk.music.data.ref.MediaRef
import timber.log.Timber

fun MediaDescription.Builder._setMediaUri(metaExtras: MediaMeta, uri: Uri): MediaDescription.Builder {
    metaExtras.mediaUri = uri
    if (isAtLeastApi23) {
        this.setMediaUri(uri)
    }
    return this
}

fun MediaBrowser.MediaItem._getMediaUri(): Uri {
    return this.description._getMediaUri()
}

fun MediaDescription._getMediaUri(): Uri {
    return if (isAtLeastApi23) {
        this.mediaUri
    } else {
        val metaExtras = MediaMeta.from(this)
        metaExtras.mediaUri
    }
}

fun MediaBrowser.MediaItem._getDisplayName(): String {
    return this.description._getDisplayName()
}

fun MediaDescription._getDisplayName(): String {
    val metaExtras = MediaMeta.from(this)
    var mediaTitle = metaExtras.displayName
    if (mediaTitle == "") {
        Timber.e("MediaTitle not set in %s", this._getMediaUri())
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
    if (isAtLeastApi23) {
        bob.setMediaUri(this.mediaUri)
    }
    return bob
}

/**
 * @return a new MediaItem cloned from this item. Information that may have been changed by an
 * external program and differing in {other} is updated and the dirty flag is set.
 */
fun MediaBrowser.MediaItem._recocileWith(other: MediaBrowser.MediaItem): MediaBrowser.MediaItem {
    val bob = this.description._newBuilder()
    val meta = MediaMeta.from(this.description)
    if (other.description.title != this.description.title) {
        bob.setTitle(other.description.title)
        meta.dirty = true
    }
    if (other.description.subtitle != this.description.subtitle) {
        bob.setSubtitle(other.description.subtitle)
        meta.dirty = true
    }
    val otherMeta = MediaMeta.from(other.description)
    if (otherMeta.displayName != meta.displayName) {
        meta.displayName = otherMeta.displayName
        meta.dirty = true
    }
    if (otherMeta.size != meta.size) {
        meta.size = otherMeta.size
        meta.dirty = true
    }
    if (otherMeta.lastModified != meta.lastModified) {
        meta.lastModified = otherMeta.lastModified
        meta.dirty = true
    }
    if (otherMeta.documentFlags != meta.documentFlags) {
        meta.documentFlags = otherMeta.documentFlags
        meta.dirty = true
    }
    return newMediaItem(bob, meta)
}

fun newMediaItem(bob: MediaDescription.Builder, meta: MediaMeta): MediaBrowser.MediaItem {
    return MediaBrowser.MediaItem(bob.setExtras(meta.meta).build(), meta.mediaItemFlags)
}

fun MediaBrowser.MediaItem._getMediaRef(): MediaRef {
    return MediaRef.parse(this.mediaId)
}

fun MediaBrowser.MediaItem._likelyDocument(): Boolean {
    return MediaRef.extractKind(this.mediaId) == MEDIA_KIND_DOCUMENT
}

fun MediaDescription.Builder._setMediaMeta(mediaMeta: MediaMeta): MediaDescription.Builder {
    return this.setExtras(mediaMeta.meta)
}

fun MediaBrowser.MediaItem._getMediaMeta(): MediaMeta {
    return MediaMeta.from(this.description)
}