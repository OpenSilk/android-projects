package org.opensilk.media

import android.media.MediaDescription
import android.media.browse.MediaBrowser.MediaItem
import android.util.JsonReader
import android.util.JsonWriter
import java.io.StringReader
import java.io.StringWriter

const val UPNP_DEVICE = "upnp_device"
const val UPNP_FOLDER = "upnp_folder"
const val UPNP_VIDEO = "upnp_video"
const val UPNP_MUSIC_TRACK = "upnp_music_track"
const val UPNP_AUDIO = "upnp_audio"
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
internal interface MediaIdTransformer<T> {
    val kind: String
    val version: Int
    fun write(jw: JsonWriter, item: T)
    fun read(jr: JsonReader, version: Int): T
}

internal fun <T> writeJson(transformer: MediaIdTransformer<T>, item: T): String {
    return StringWriter().use {
        val jw = JsonWriter(it)
        jw.beginObject()
        jw.name(transformer.kind)
        jw.beginObject()
        when (transformer.version) {
            1 -> jw.name("v1")
            else -> jw.name("v0")
        }
        jw.beginObject()
        transformer.write(jw, item)
        jw.endObject()
        jw.endObject()
        jw.endObject()
        return@use it.toString()
    }
}

private fun readVersion(jr: JsonReader): Int {
    jr.beginObject()
    val version = when (jr.nextName()) {
        "v1" -> 1
        else -> 0
    }
    jr.beginObject()
    return version
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
                    mediaId = UpnpDeviceTransformer.read(jr, readVersion(jr))
                }
                UPNP_FOLDER -> {
                    mediaId = UpnpFolderTransformer.read(jr, readVersion(jr))
                }
                UPNP_VIDEO -> {
                    mediaId = UpnpVideoTransformer.read(jr, readVersion(jr))
                }
                UPNP_MUSIC_TRACK -> {
                    mediaId = UpnpMusicTrackTransformer.read(jr, readVersion(jr))
                }
                UPNP_AUDIO -> {
                    mediaId = UpnpAudioTransformer.read(jr, readVersion(jr))
                }
                DOCUMENT -> {
                    mediaId = DocumentIdTransformer.read(jr, readVersion(jr))
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
            bob.setTitle(meta.title.elseIfBlank(meta.originalTitle))
                    .setSubtitle(meta.subtitle)
                    .setIconUri(meta.artworkUri)
                    .setMediaId(id.json)
                    .setExtras(bundle(KEY_MEDIA_URI, meta.mediaUri)
                            ._putLong(KEY_DURATION, meta.duration))
        }
        is UpnpFolderRef -> {
            bob.setTitle(meta.title)
                    .setMediaId(id.json)
        }
        is UpnpDeviceRef -> {
            bob.setTitle(meta.title)
                    .setSubtitle(meta.subtitle)
                    .setIconUri(meta.artworkUri)
                    .setMediaId(id.json)
        }
        is UpnpMusicTrackRef -> {
            bob.setTitle(meta.title.elseIfBlank(meta.originalTitle))
                    .setSubtitle(meta.artist.elseIfBlank(meta.creator))
                    .setIconUri(meta.artworkUri.elseIfEmpty(meta.originalArtworkUri))
                    .setMediaId(id.json)
                    .setExtras(bundle(KEY_MEDIA_URI, meta.mediaUri)
                            ._putLong(KEY_DURATION, meta.duration))
        }
        is UpnpAudioRef -> {
            bob.setTitle(meta.title)
                    .setSubtitle(meta.creator)
                    .setMediaId(id.json)
        }
        is DirectoryDocumentRef -> {
            bob.setTitle(meta.title.elseIfBlank(meta.displayName))
                    .setMediaId(id.json)
        }
        is VideoDocumentRef -> {
            bob.setTitle(meta.title.elseIfBlank(meta.displayName))
                    .setSubtitle(meta.subtitle)
                    .setIconUri(meta.artworkUri)
                    .setMediaId(id.json)
                    .setExtras(bundle(KEY_MEDIA_URI, id.mediaUri))
        }
        else -> TODO("Unsupported mediaRef ${this::javaClass.name}")
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
        is UpnpMusicTrackRef -> {
            MediaItem(toMediaDescription(), MediaItem.FLAG_PLAYABLE)
        }
        is UpnpAudioRef -> {
            MediaItem(toMediaDescription(), MediaItem.FLAG_PLAYABLE)
        }
        is DirectoryDocumentRef -> {
            MediaItem(toMediaDescription(), MediaItem.FLAG_BROWSABLE)
        }
        is VideoDocumentRef -> {
            MediaItem(toMediaDescription(), MediaItem.FLAG_PLAYABLE)
        }
        else -> TODO("Unsupported mediaRef ${this::javaClass.name}")
    }
}
