package org.opensilk.media

import android.graphics.Movie
import android.media.MediaDescription
import android.media.browse.MediaBrowser
import android.media.tv.TvContract
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import java.util.HashMap
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

const val MIME_TYPE_DIR = DocumentsContract.Document.MIME_TYPE_DIR
const val MIME_TYPE_CONTENT_DIRECTORY = "vnd.opensilk.org/ContentDirectory"
const val MIME_TYPE_MOVIE = "vnd.opensilk.org/movie"
const val MIME_TYPE_TV_SERIES = "vnd.opensilk.org/series"
const val MIME_TYPE_TV_EPISODE = "vnd.opensilk.org/episode"


/**
 * Created by drew on 6/26/16.
 */
data class MediaMeta
constructor(
        internal val meta: Bundle = Bundle()
)
{

    companion object {
        @JvmStatic fun empty(): MediaMeta = MediaMeta()
        @JvmStatic fun copy(m: MediaMeta) = MediaMeta(Bundle(m.meta))
        @JvmStatic fun from(desc: MediaDescription) = MediaMeta(desc.extras)
    }

    /**
     * Database rowid of this item, table should be inferred from other information
     */
    var rowId: Long by LongVal

    /**
     * [android.provider.DocumentsContract.Document.COLUMN_DOCUMENT_ID]
     */
    @Deprecated("Use custom mediaId instead")
    var documentId: String by StringVal
    /**
     *
     */
    @Deprecated("Use custom mediaId instead")
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
        get() = mimeType === DocumentsContract.Document.MIME_TYPE_DIR
    /**
     *
     */
    val isAudio: Boolean
        get() = mimeType.startsWith("audio") || mimeType.contains("flac") || mimeType.contains("ogg")
    /**
     *
     */
    val isAlbum: Boolean
        get() = mimeType === MediaStore.Audio.Albums.ENTRY_CONTENT_TYPE
    /**
     *
     */
    val isArtist: Boolean
        get() = mimeType === MediaStore.Audio.Artists.ENTRY_CONTENT_TYPE
    /**
     *
     */
    val isGenre: Boolean
        get() = mimeType === MediaStore.Audio.Genres.ENTRY_CONTENT_TYPE
    /**
     *
     */
    val isPlaylist: Boolean
        get() = mimeType === MediaStore.Audio.Playlists.ENTRY_CONTENT_TYPE
    /**
     *
     */
    val isTvSeries: Boolean
        get() = mimeType === TvContract.Programs.CONTENT_TYPE
    /**
     *
     */
    val isTvEpisode: Boolean
        get() = mimeType === TvContract.Programs.CONTENT_ITEM_TYPE
    /**
     *
     */
    val isMovie: Boolean
        get() = mimeType === MIME_TYPE_MOVIE
    /**
     *
     */
    val isVideo: Boolean
        get() = mimeType.startsWith("video")
    /**
     *
     */
    val isContentDiretory: Boolean
        get() = mimeType === MIME_TYPE_CONTENT_DIRECTORY
    /**
     *
     */
    var isParsed: Boolean by BoolVal
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
            MIME_TYPE_CONTENT_DIRECTORY -> {
                MediaBrowser.MediaItem.FLAG_BROWSABLE
            }
            MIME_TYPE_MOVIE -> {
                MediaBrowser.MediaItem.FLAG_PLAYABLE
            }
            else -> if (isAudio || isVideo) MediaBrowser.MediaItem.FLAG_PLAYABLE else 0
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
    var subtitle: String by StringVal
    /**
     *
     */
    var overview: String by StringVal
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
     * String: human friendly release date
     */
    var releaseDate: String by StringVal
    /**
     * Long: Bitrate of track
     */
    var bitrate: Long by LongVal
    /**
     * Long: ms duration of track
     */
    var duration: Long by LongVal
    /**
     * String: video resolution
     */
    var resolution: String by StringVal
    /**
     * Int: value > 0 if part of compilation
     */
    var isCompilation: Boolean by BoolVal
    /**
     * Int: position of track in album / playlist / etc
     */
    var trackNumber: Int by IntVal
    /**
     * Int: total number of tracks (on disk or playlist or whatever)
     */
    var trackCount: Int by IntVal
    /**
     * Int: disc number for track (defaults to 1)
     */
    var discNumber: Int by IntVal
    /**
     * Int: total number of discs
     */
    var discCount: Int by IntVal
    /**
     * Uri: (any scheme) where jpg/png/etc can be fetched containing cover art
     */
    var artworkUri: Uri by UriVal
    /**
     * Int: android resource id
     */
    var artworkResourceId: Int by IntVal
    /**
     * Uri: (any scheme) where jpg/png/etc can be fetched containing image of artist or group or whatever
     */
    var backdropUri: Uri by UriVal
    /**
     *
     */
    var mediaId: String by StringVal
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
     *
     */
    val mediaHeadersMap: Map<String, String>
        get() {
            if (mediaHeaders.isBlank()) return emptyMap()
            return mediaHeaders.split("\n").map {
                it.split(":")
            }.filter {
                it.size == 2
            }.map {
                Pair(it[0].trim(), it[1].trim())
            }.toMap()
        }
    /**
     * Long: (internal use) ms since epoch item was added to index
     */
    var dateAdded: Long by LongVal
    /**
     * Long: last saved playback position
     */
    var lastPlaybackPosition: Long by LongVal
    /**
     * Int: season number
     */
    var seasonNumber: Int by IntVal
    /**
     * Int: episode number
     */
    var episodeNumber: Int by IntVal
    /**
     *
     */
    val extras: Bundle by lazy {
        if (!meta.containsKey("__extras")) {
            meta.putBundle("__extras", Bundle())
        }
        return@lazy meta.getBundle("__extras")
    }

    /*
     * Scratch space until better solution are found
     */
    /**
     * episode id
     */
    var __internal1: String by StringVal
    /**
     * season id
     */
    var __internal2: String by StringVal
    /**
     * movie id
     */
    var __internal3: String by StringVal
    /**
     * server id
     */
    var __internal4: String by StringVal


    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        if (other !is MediaMeta) return false
        return other.mediaId == mediaId
    }
}

fun String?.elseIfBlank(alternate: String): String {
    return if (isNullOrBlank()) {
        alternate
    } else {
        this ?: alternate
    }
}

fun MediaMeta.toMediaItem(): MediaBrowser.MediaItem {
    val bob = MediaDescription.Builder()
    if (mediaId.isEmpty() || title.isEmpty()) {
        throw IllegalArgumentException("Must set mediaId and title")
    }
    if ((isVideo || isAudio) && mediaUri == Uri.EMPTY) {
        throw IllegalArgumentException("mediaUri must be set on playable files")
    }
    bob.setMediaId(mediaId)
            .setTitle(title)
            .setSubtitle(subtitle)
            .setIconUri(artworkUri)
            ._setMediaMeta(this)
    if (Build.VERSION.SDK_INT >= 23) {
        bob.setMediaUri(mediaUri)
    }
    return MediaBrowser.MediaItem(bob.build(), mediaItemFlags)
}

@Deprecated("Temp until usages in MediaMetaExtras are fixed")
fun MediaMeta.getBundle(): Bundle {
    return meta
}

@Deprecated("Use MediaMeta.toMediaItem()")
fun MediaDescription.Builder._setMediaMeta(mediaMeta: MediaMeta): MediaDescription.Builder {
    return this.setExtras(mediaMeta.meta)
}

fun MediaBrowser.MediaItem._getMediaMeta(): MediaMeta {
    return MediaMeta.from(this.description)
}

fun MediaDescription._getMediaMeta(): MediaMeta {
    return MediaMeta.from(this)
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
        return thisRef.meta.getLong(property.name, 0);
    }

    operator fun setValue(thisRef: MediaMeta, property: KProperty<*>, value: Long) {
        thisRef.meta.putLong(property.name, value)
    }
}

private object IntVal {
    operator fun getValue(thisRef: MediaMeta, property: KProperty<*>): Int {
        return thisRef.meta.getInt(property.name, 0);
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

