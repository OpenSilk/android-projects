package org.opensilk.media

import android.media.MediaDescription
import android.media.browse.MediaBrowser
import android.net.Uri
import android.util.JsonReader
import android.util.JsonWriter
import java.io.StringReader
import java.io.StringWriter

const val UPNP_DEVICE = "upnp_device"
const val UPNP_FOLDER = "upnp_folder"
const val UPNP_VIDEO = "upnp_video"

const val KEY_MEDIA_URI = "media_uri"
const val KEY_DURATION = "media_duration"

/**
 * A generic media id interface for use with the media controller api
 *
 * Created by drew on 5/29/17.
 */
interface MediaId {
    val json: String
}

/**
 * Contains the media id and metadata
 */
interface MediaRef {
    val id: MediaId
}

/**
 * Helper for transforming the media id
 */
private interface MediaIdTransformer<T> {
    val kind: String
    val version: Int
    fun write(jw: JsonWriter, item: T)
    fun read(jr: JsonReader): T
}

private fun <T> writeJson(transformer: MediaIdTransformer<T>, item: T): String {
    return StringWriter().use {
        val jw = JsonWriter(it)
        jw.beginObject()
        jw.name(transformer.kind)
        jw.beginObject()
        jw.name("ver").value(transformer.version)
        transformer.write(jw, item)
        jw.endObject()
        jw.endObject()
        return@use it.toString()
    }
}

fun parseMediaId(json: String): MediaId {
    return JsonReader(StringReader(json)).use { jr ->
        var mediaId: MediaId? = null
        jr.beginObject()
        while (jr.hasNext()) {
            when (jr.nextName()) {
                UPNP_DEVICE -> {
                    jr.beginObject()
                    mediaId = UpnpDeviceTransformer.read(jr)
                    jr.endObject()
                }
                UPNP_FOLDER -> {
                    jr.beginObject()
                    mediaId = UpnpFolderTransformer.read(jr)
                    jr.endObject()
                }
                UPNP_VIDEO -> {
                    jr.beginObject()
                    mediaId = UpnpFolderTransformer.read(jr)
                    jr.endObject()
                }
                else -> jr.skipValue()
            }
        }
        jr.endObject()
        return@use mediaId!!
    }
}

fun MediaRef.toMediaDescription(): MediaDescription {
    val bob = MediaDescription.Builder()
    when (this) {
        is UpnpVideoRef -> {
            bob.setTitle(meta.title.elseIfBlank(meta.mediaTitle))
                    .setSubtitle(meta.subtitle)
                    .setIconUri(meta.artworkUri)
                    .setMediaId(id.json)
                    .setExtras(bundle(KEY_MEDIA_URI, meta.mediaUri)
                            ._putLong(KEY_DURATION, meta.duration))
        }
        is UpnpFolderRef -> {
            bob.setTitle(meta.title)
                    .setSubtitle(meta.subtitle)
                    .setIconUri(meta.artworkUri)
        }
        is UpnpDeviceRef -> {
            bob.setTitle(meta.title)
                    .setSubtitle(meta.subtitle)
                    .setIconUri(meta.artworkUri)
        }
        else -> TODO("Unsupported mediaRef ${this::class}")
    }
    return bob.build()
}

fun MediaRef.toMediaItem(): MediaBrowser.MediaItem {
    return when (this) {
        is UpnpVideoRef -> {
            MediaBrowser.MediaItem(toMediaDescription(), MediaBrowser.MediaItem.FLAG_PLAYABLE)
        }
        is UpnpFolderRef -> {
            MediaBrowser.MediaItem(toMediaDescription(), MediaBrowser.MediaItem.FLAG_BROWSABLE)
        }
        is UpnpDeviceRef -> {
            MediaBrowser.MediaItem(toMediaDescription(), MediaBrowser.MediaItem.FLAG_BROWSABLE)
        }
        else -> TODO("Unsupported mediaRef ${this::class}")
    }
}

data class UpnpDeviceId(val deviceId: String): MediaId {
    override val json: String by lazy {
        writeJson(UpnpDeviceTransformer, this)
    }
}

data class UpnpDeviceRef(override val id: UpnpDeviceId,
                         val meta: UpnpDeviceMeta): MediaRef

data class UpnpDeviceMeta(
        val title: String,
        val subtitle: String = "",
        val artworkUri: Uri = Uri.EMPTY,
        val updateId: Long = 0
)

private object UpnpDeviceTransformer: MediaIdTransformer<UpnpDeviceId> {
    override val kind: String = UPNP_DEVICE
    override val version: Int = 1

    override fun write(jw: JsonWriter, item: UpnpDeviceId) {
        jw.name("dev").value(item.deviceId)
    }

    override fun read(jr: JsonReader): UpnpDeviceId {
        var dev = ""
        while (jr.hasNext()) {
            when (jr.nextName()) {
                "dev" -> dev = jr.nextString()
                else -> jr.skipValue()
            }
        }
        return UpnpDeviceId(dev)
    }
}

data class UpnpFolderId(val deviceId: String, val folderId: String): MediaId {
    override val json: String by lazy {
        writeJson(UpnpFolderTransformer, this)
    }
}

data class UpnpFolderRef(override val id: UpnpFolderId,
                         val parentId: UpnpFolderId,
                         val meta: UpnpFolderMeta): MediaRef

data class UpnpFolderMeta(
        val title: String,
        val subtitle: String = "",
        val artworkUri: Uri = Uri.EMPTY
)

private object UpnpFolderTransformer: MediaIdTransformer<UpnpFolderId> {
    override val kind: String = UPNP_FOLDER
    override val version: Int = 1

    override fun write(jw: JsonWriter, item: UpnpFolderId) {
        jw.name("dev").value(item.deviceId)
        jw.name("fol").value(item.folderId)
    }

    override fun read(jr: JsonReader): UpnpFolderId {
        var dev = ""
        var fol = ""
        while (jr.hasNext()) {
            when (jr.nextName()) {
                "dev" -> dev = jr.nextString()
                "fol" -> fol = jr.nextString()
                else -> jr.skipValue()
            }
        }
        return UpnpFolderId(dev, fol)
    }
}

data class UpnpVideoId(val deviceId: String, val itemId: String): MediaId {
    override val json: String by lazy {
        writeJson(UpnpVideoTransformer, this)
    }
}

data class UpnpVideoRef(override val id: UpnpVideoId,
                        val parentId: UpnpFolderId,
                        val tvEpisodeId: TvEpisodeId? = null,
                        val movieId: MovieId? = null,
                        val meta: UpnpVideoMeta): MediaRef

data class UpnpVideoMeta(
        val title: String = "",
        val subtitle: String = "",
        val artworkUri: Uri = Uri.EMPTY,
        val backdropUri: Uri = Uri.EMPTY,
        val mediaTitle: String,
        val mediaUri: Uri,
        val mimeType: String,
        val duration: Long = 0,
        val size: Long = 0,
        val bitrate: Long = 0,
        val resolution: String = ""
)

private object UpnpVideoTransformer: MediaIdTransformer<UpnpVideoId> {
    override val kind: String = UPNP_VIDEO
    override val version: Int = 1

    override fun write(jw: JsonWriter, item: UpnpVideoId) {
        jw.name("dev").value(item.deviceId)
        jw.name("itm").value(item.itemId)
    }

    override fun read(jr: JsonReader): UpnpVideoId {
        var dev = ""
        var itm = ""
        while (jr.hasNext()) {
            when (jr.nextName()) {
                "dev" -> dev = jr.nextString()
                "itm" -> itm = jr.nextString()
                else -> jr.skipValue()
            }
        }
        return UpnpVideoId(dev, itm)
    }
}

data class MovieId(val movieId: Long): MediaId {
    override val json: String
        get() = TODO("not implemented")
}

data class MovieRef(override val id: MovieId, val meta: MovieMeta): MediaRef

data class MovieMeta(
        val title: String,
        val overview: String = "",
        val releaseDate: String = "",
        val posterPath: String = "",
        val backdropPath: String = ""
)

data class TvSeriesId(val seriesId: Long): MediaId {
    override val json: String
        get() = TODO("not implemented")
}

data class TvSeriesRef(override val id: TvSeriesId, val meta: TvSeriesMeta): MediaRef

data class TvSeriesMeta(
        val title: String,
        val overview: String = "",
        val releaseDate: String = "",
        val posterPath: String = "",
        val backdropPath: String = ""
)

data class TvEpisodeId(val episodeId: Long, val seriesId: Long): MediaId {
    override val json: String
        get() = TODO("not implemented")
}

data class TvEpisodeRef(override val id: TvEpisodeId, val meta: TvEpisodeMeta): MediaRef

data class TvEpisodeMeta(
        val title: String,
        val overview: String = "",
        val releaseDate: String = "",
        val episodeNumber: Int,
        val seasonNumber: Int,
        val posterPath: String = "",
        val backdropPath: String = ""
)

data class TvImageId(val imageId: Long, val seriesId: Long): MediaId {
    override val json: String
        get() = TODO("not implemented")
}

data class TvImageRef(override val id: TvImageId, val meta: TvImageMeta): MediaRef

data class TvImageMeta(
        val path: String,
        val type: String,
        val subType: String = "",
        val rating: Float = 0f,
        val ratingCount: Int = 0,
        val resolution: String = ""
)

data class MovieImageId(val imageId: Long, val movieId: Long): MediaId {
    override val json: String
        get() = TODO("not implemented")
}

data class MovieImageRef(override val id: MovieImageId, val meta: MovieImageMeta): MediaRef

data class MovieImageMeta(
        val path: String,
        val type: String,
        val rating: Float = 0f,
        val ratingCount: Int = 0,
        val resolution: String = ""
)

/**
 *
 */
fun isLikelyJson(str: String): Boolean {
    return str.first() == '{' || str.first() == '['
}

fun Uri.isEmpty(): Boolean {
    return this == Uri.EMPTY
}