package org.opensilk.media

import android.graphics.Bitmap
import android.media.MediaDescription
import android.media.MediaMetadata
import android.media.MediaMetadata.*
import android.media.browse.MediaBrowser
import android.media.session.MediaSession
import android.net.Uri
import android.os.Build
import timber.log.Timber

/**
 * Created by drew on 6/26/16.
 */
/*
 * Extensions for MediaItem and MediaDescription
 */

@Deprecated("Use MediaMeta.toMediaItem()")
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
    return if (metaExtras.displayName != "") {
        metaExtras.displayName
    } else {
        title.toString()
    }
}

@Deprecated("Use MediaMeta.toMediaItem()")
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

@Deprecated("Use MediaMeta.toMediaItem()")
fun MediaBrowser.MediaItem._copy(bob: MediaDescription.Builder, meta: MediaMeta): MediaBrowser.MediaItem {
    return MediaBrowser.MediaItem(bob._setMediaMeta(meta).build(), this.flags)
}

@Deprecated("Use MediaMeta.toMediaItem()")
fun newMediaItem(bob: MediaDescription.Builder, meta: MediaMeta): MediaBrowser.MediaItem {
    return MediaBrowser.MediaItem(bob.setExtras(meta.meta).build(), meta.mediaItemFlags)
}

/**
 * Tries and tries and tries to find a suitable display name
 */
fun MediaMetadata._title(): String {
    return this.getString(METADATA_KEY_DISPLAY_TITLE) ?:
            this.getString(METADATA_KEY_TITLE) ?:
            this.description.title?.toString() ?:
            this.description._getMediaTitle()
}

fun MediaMetadata._subtitle(): String {
    return this.getString(METADATA_KEY_DISPLAY_SUBTITLE) ?: ""
}

fun MediaMetadata._artistName(): String {
    return this.getString(METADATA_KEY_ARTIST) ?: ""
}

fun MediaMetadata._albumName(): String {
    return this.getString(METADATA_KEY_ALBUM) ?: ""
}

fun MediaMetadata._albumArtistName(): String {
    return this.getString(METADATA_KEY_ALBUM_ARTIST) ?: ""
}

fun MediaMetadata._iconUri(): Uri? {
    var uri: String? = this.getString(METADATA_KEY_DISPLAY_ICON_URI)
    if (uri == null) {
        uri = this.getString(METADATA_KEY_ART_URI)
    }
    if (uri == null) {
        uri = this.getString(METADATA_KEY_ALBUM_ART_URI)
    }
    return if (uri != null) Uri.parse(uri) else null
}

fun MediaMetadata._icon(): Bitmap? {
    var bitmap: Bitmap? = this.getBitmap(METADATA_KEY_DISPLAY_ICON)
    if (bitmap == null) {
        bitmap = this.getBitmap(METADATA_KEY_ART)
    }
    if (bitmap == null) {
        bitmap = this.getBitmap(METADATA_KEY_ALBUM_ART)
    }
    return bitmap
}


fun MediaBrowser.notConnected(): Boolean {
    return !isConnected
}
