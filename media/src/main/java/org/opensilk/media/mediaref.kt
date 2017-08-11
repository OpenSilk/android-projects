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
internal interface MediaIdTransformer<T> {
    val kind: String
    val version: Int
    fun write(jw: JsonWriter, item: T)
    fun read(jr: JsonReader): T
}

internal fun <T> writeJson(transformer: MediaIdTransformer<T>, item: T): String {
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
        is DocumentRef -> {
            MediaItem(toMediaDescription(), if (isDirectory)
                MediaItem.FLAG_BROWSABLE else MediaItem.FLAG_PLAYABLE)
        }
        else -> TODO("Unsupported mediaRef ${this::javaClass.name}")
    }
}
