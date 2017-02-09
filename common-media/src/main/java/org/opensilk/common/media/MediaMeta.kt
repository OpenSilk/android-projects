package org.opensilk.common.media

import android.media.MediaDescription
import android.media.browse.MediaBrowser
import android.net.Uri
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.provider.DocumentsContract
import android.provider.MediaStore
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

/**
 * Created by drew on 6/26/16.
 */
data class MediaMeta
private constructor(
        internal val meta: Bundle = Bundle()
)
{

    companion object {
        @JvmStatic fun empty(): MediaMeta = MediaMeta()
        @JvmStatic fun copy(m: MediaMeta) = MediaMeta(Bundle(m.meta))
        @JvmStatic fun from(desc: MediaDescription) = MediaMeta(desc.extras)
    }

    /**
     * [android.provider.DocumentsContract.Document.COLUMN_DOCUMENT_ID]
     */
    var documentId: String by StringVal
    /**
     *
     */
    var documentAuthority: String by StringVal
    /**
     * [android.provider.DocumentsContract.Document.COLUMN_DISPLAY_NAME]
     */
    var displayName: String by StringVal
    /**
     * [android.provider.DocumentsContract.Document.COLUMN_MIME_TYPE]
     */
    var mimeType: String by StringVal
    /**
     *
     */
    val isDirectory: Boolean
        get() = mimeType == DocumentsContract.Document.MIME_TYPE_DIR;
    /**
     *
     */
    val isAudio: Boolean
        get() = mimeType.startsWith("audio") || mimeType.contains("flag") || mimeType.contains("ogg")
    /**
     *
     */
    val isAlbum: Boolean
        get() = mimeType == MediaStore.Audio.Albums.ENTRY_CONTENT_TYPE
    /**
     *
     */
    val isArtist: Boolean
        get() = mimeType == MediaStore.Audio.Artists.ENTRY_CONTENT_TYPE
    /**
     *
     */
    val isGenre: Boolean
        get() = mimeType == MediaStore.Audio.Genres.ENTRY_CONTENT_TYPE
    /**
     *
     */
    val isPlaylist: Boolean
        get() = mimeType == MediaStore.Audio.Playlists.ENTRY_CONTENT_TYPE
    /**
     *
     */
    val isParsed: Boolean by BoolVal
    /**
     * whether item is browsable, playable or both
     */
    val mediaItemFlags: Int
        get() = when (mimeType) {
            DocumentsContract.Document.MIME_TYPE_DIR,
            MediaStore.Audio.Albums.ENTRY_CONTENT_TYPE,
            MediaStore.Audio.Artists.ENTRY_CONTENT_TYPE,
            MediaStore.Audio.Genres.ENTRY_CONTENT_TYPE,
            MediaStore.Audio.Playlists.ENTRY_CONTENT_TYPE -> {
                MediaBrowser.MediaItem.FLAG_BROWSABLE or MediaBrowser.MediaItem.FLAG_PLAYABLE
            }
            else -> if (isAudio) MediaBrowser.MediaItem.FLAG_PLAYABLE else 0
        }
    /**
     * [android.provider.DocumentsContract.Document.COLUMN_SIZE]
     */
    var size: Long by LongVal
    /**
     * [android.provider.DocumentsContract.Document.COLUMN_LAST_MODIFIED]
     */
    var lastModified: Long by LongVal
    /**
     * bitmask of [android.provider.DocumentsContract.Document.COLUMN_FLAGS]
     */
    var documentFlags: Int by IntVal
    /**
     *
     */
    val isDeleteable: Boolean
        get() = (documentFlags and DocumentsContract.Document.FLAG_SUPPORTS_DELETE) != 0
    /**
     *
     */
    var title: String by StringVal
    /**
     *
     */
    var albumName: String by StringVal
    /**
     *
     */
    var artistName: String by StringVal
    /**
     *
     */
    var albumArtistName: String by StringVal
    /**
     * String: name of genre this [Track] belongs to
     */
    var genreName: String by StringVal
    /**
     * Long: Year Album/track/etc was released
     */
    var releaseYear: Int by IntVal
    /**
     * Long: Bitrate of track
     */
    var bitrate: Long by LongVal
    /**
     * Long: ms duration of track
     */
    var duration: Long by LongVal
    /**
     * Int: value > 0 if part of compilation
     */
    var isCompilation: Boolean by BoolVal
    /**
     * Int: position of track in album / playlist / etc
     */
    var trackNumber: Int by IntVal
    /**
     * Int: disc number for track (defaults to 1)
     */
    var discNumber: Int by IntVal
    /**
     * Uri: (any scheme) where jpg/png/etc can be fetched containing cover art
     */
    var artworkUri: Uri by UriVal
    /**
     * Uri: (any scheme) where jpg/png/etc can be fetched containing image of artist or group
     */
    var artistArtworkUri: Uri by UriVal
    /**
     * Uri: media uri
     */
    var mediaUri: Uri by UriVal
    /**
     * Uri: if of parent [android.media.browse.MediaBrowser.MediaItem]
     */
    var parentMediaId: String by StringVal
    /**
     * String: new line separated headers needed for accessing [mediaUri] over http/s
     */
    var mediaHeaders: String by StringVal
    /**
     * Long: (internal use) ms since epoch item was added to index
     */
    var dateAdded: Long by LongVal

}

fun MediaDescription.Builder._setMediaMeta(mediaMeta: MediaMeta): MediaDescription.Builder {
    return this.setExtras(mediaMeta.meta)
}

fun MediaBrowser.MediaItem._getMediaMeta(): MediaMeta {
    return MediaMeta.from(this.description)
}

private object StringVal {
    operator fun getValue(thisRef: MediaMeta, property: KProperty<*>): String {
        return thisRef.meta.getString(property.name, "");
    }

    operator fun setValue(thisRef: MediaMeta, property: KProperty<*>, value: String) {
        thisRef.meta.putString(property.name, value)
    }
}

private object LongVal {
    operator fun getValue(thisRef: MediaMeta, property: KProperty<*>): Long {
        return thisRef.meta.getLong(property.name, -1);
    }

    operator fun setValue(thisRef: MediaMeta, property: KProperty<*>, value: Long) {
        thisRef.meta.putLong(property.name, value)
    }
}

private object IntVal {
    operator fun getValue(thisRef: MediaMeta, property: KProperty<*>): Int {
        return thisRef.meta.getInt(property.name, -1);
    }

    operator fun setValue(thisRef: MediaMeta, property: KProperty<*>, value: Int) {
        thisRef.meta.putInt(property.name, value)
    }
}

private object UriVal {
    operator fun getValue(thisRef: MediaMeta, property: KProperty<*>): Uri {
        return thisRef.meta.getParcelable(property.name) ?: Uri.EMPTY;
    }

    operator fun setValue(thisRef: MediaMeta, property: KProperty<*>, value: Uri) {
        thisRef.meta.putParcelable(property.name, value)
    }
}

private object BoolVal {
    operator fun getValue(thisRef: MediaMeta, property: KProperty<*>): Boolean {
        return thisRef.meta.getBoolean(property.name)
    }

    operator fun setValue(thisRef: MediaMeta, property: KProperty<*>, value: Boolean) {
        thisRef.meta.putBoolean(property.name, value)
    }
}

