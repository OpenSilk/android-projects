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
 * Extensions for MediaItem and MediaDescription
 */

fun MediaBrowser.MediaItem._getMediaUri(): Uri {
    return this.description._getMediaUri()
}

fun MediaDescription._getMediaUri(): Uri {
    return extras.getParcelable(KEY_MEDIA_URI)
}

fun MediaMetadata._title(): String {
    return this.description.title?.toString() ?: ""
}

fun MediaMetadata._subtitle(): String {
    return this.description.subtitle?.toString() ?: ""
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

fun String?.elseIfBlank(alternate: String): String {
    return if (this.isNullOrBlank()) {
        alternate
    } else {
        this!!
    }
}

fun isLikelyJson(str: String): Boolean {
    return str.first() == '{' || str.first() == '['
}

fun Uri?.isEmpty(): Boolean {
    return this == null || this == Uri.EMPTY
}

fun Uri?.elseIfEmpty(other: Uri): Uri {
    return if (this == null || this == Uri.EMPTY) other else this
}
