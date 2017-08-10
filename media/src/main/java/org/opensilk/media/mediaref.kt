package org.opensilk.media

import android.media.MediaDescription
import android.media.browse.MediaBrowser.MediaItem
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.util.JsonReader
import android.util.JsonWriter
import java.io.StringReader
import java.io.StringWriter

const val UPNP_DEVICE = "upnp_device"
const val UPNP_FOLDER = "upnp_folder"
const val UPNP_VIDEO = "upnp_video"
const val DOCUMENT = "document"

const val KEY_MEDIA_URI = "media_uri"
const val KEY_DURATION = "media_duration"

object NoMediaId: MediaId {
    override val json: String = ""
}

object NoMediaRef: MediaRef {
    override val id: MediaId = NoMediaId
}

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

/**
 * Recovers a MediaId from its json representation
 * TODO not all MediaIds are represented here
 */
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
                    mediaId = UpnpVideoTransformer.read(jr)
                    jr.endObject()
                }
                DOCUMENT -> {
                    jr.beginObject()
                    mediaId = DocumentIdTransformer.read(jr)
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
                    .setMediaId(id.json)
        }
        is UpnpDeviceRef -> {
            bob.setTitle(meta.title)
                    .setSubtitle(meta.subtitle)
                    .setIconUri(meta.artworkUri)
                    .setMediaId(id.json)
        }
        is DocumentRef -> {
            bob.setTitle(meta.title.elseIfBlank(meta.displayName))
                    .setSubtitle(meta.subtitle)
                    .setIconUri(meta.artworkUri)
                    .setMediaId(id.json)
        }
        else -> TODO("Unsupported mediaRef ${this::class}")
    }
    return bob.build()
}

fun MediaRef.toMediaItem(): MediaItem {
    return when (this) {
        is UpnpVideoRef -> {
            MediaItem(toMediaDescription(), MediaItem.FLAG_PLAYABLE)
        }
        is UpnpFolderRef -> {
            MediaItem(toMediaDescription(), MediaItem.FLAG_BROWSABLE)
        }
        is UpnpDeviceRef -> {
            MediaItem(toMediaDescription(), MediaItem.FLAG_BROWSABLE)
        }
        is DocumentRef -> {
            MediaItem(toMediaDescription(), if (isDirectory)
                MediaItem.FLAG_BROWSABLE else MediaItem.FLAG_PLAYABLE)
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

const val ROOTS_PARENT_ID = "\u2605G\u2605O\u2605D\u2605"

data class DocumentId(val treeUri: Uri,
                      val documentId: String = if (isTreeUri(treeUri))
                          DocumentsContract.getTreeDocumentId(treeUri) else
                          DocumentsContract.getDocumentId(treeUri),
                      val parentId: String = documentId): MediaId {

    val authority: String = treeUri.authority

    val isRoot: Boolean by lazy {
        if (isFromTree) {
            DocumentsContract.getTreeDocumentId(treeUri) == documentId
        } else {
            true
        }
    }

    val mediaUri: Uri by lazy {
        if (isFromTree) {
            DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId)
        } else {
            DocumentsContract.buildDocumentUri(authority, documentId)
        }
    }

    val childrenUri: Uri by lazy {
        if (isFromTree) {
            DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, documentId)
        } else {
            throw IllegalArgumentException("This documentId does not represent a tree")
        }
    }

    val isFromTree: Boolean by lazy {
        isTreeUri(treeUri)
    }

    override val json: String by lazy {
        writeJson(DocumentIdTransformer, this)
    }

}

private fun isTreeUri(treeUri: Uri): Boolean {
    return if (Build.VERSION.SDK_INT >= 24) {
        DocumentsContract.isTreeUri(treeUri)
    } else {
        val paths = treeUri.pathSegments
        paths.size >= 2 && "tree" == paths[0]
    }
}

data class DocumentRef(override val id: DocumentId,
                       val tvEpisodeId: TvEpisodeId? = null,
                       val movieId: MovieId? = null,
                       val meta: DocumentMeta): MediaRef {
    val isDirectory: Boolean by lazy {
        meta.mimeType == DocumentsContract.Document.MIME_TYPE_DIR
    }
    val isVideo: Boolean by lazy {
        meta.mimeType.startsWith("video", true)
    }
    val isAudio: Boolean by lazy {
        meta.mimeType.startsWith("audio", true)
                || meta.mimeType.contains("flac", true)
                || meta.mimeType.contains("ogg", true)
    }
}

data class DocumentMeta(
        val displayName: String,
        val summary: String = "",
        val mimeType: String,
        val size: Long = 0,
        val lastMod: Long = 0,
        val flags: Long = 0,
        val title: String = "",
        val subtitle: String = "",
        val artworkUri: Uri = Uri.EMPTY,
        val backdropUri: Uri = Uri.EMPTY
)

private object DocumentIdTransformer: MediaIdTransformer<DocumentId> {

    override val kind: String
        get() = DOCUMENT

    override val version: Int
        get() = 1

    override fun write(jw: JsonWriter, item: DocumentId) {
        jw.name("tree").value(item.treeUri.toString())
        jw.name("doc").value(item.documentId)
    }

    override fun read(jr: JsonReader): DocumentId {
        var treeStr = ""
        var docStr = ""
        while (jr.hasNext()) {
            when (jr.nextName()) {
                "tree" -> treeStr = jr.nextString()
                "doc" -> docStr = jr.nextString()
                else -> jr.skipValue()
            }
        }
        return DocumentId(Uri.parse(treeStr), docStr)
    }

}
