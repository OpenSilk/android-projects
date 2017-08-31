package org.opensilk.media

import android.content.Intent
import android.media.MediaDescription
import android.media.browse.MediaBrowser.MediaItem
import android.os.Bundle
import android.os.PersistableBundle
import android.util.JsonReader
import android.util.JsonWriter
import java.io.StringReader
import java.io.StringWriter

const val UPNP_DEVICE = "upnp_device"
const val UPNP_FOLDER = "upnp_folder"
const val UPNP_VIDEO = "upnp_video"
const val UPNP_MUSIC_TRACK = "upnp_music_track"
const val UPNP_AUDIO = "upnp_audio"
const val DOCUMENT_DEVICE = "document_device"
const val DOCUMENT_DIRECTORY = "document_directory"
const val DOCUMENT_VIDEO = "document_video"
const val STORAGE_DEVICE = "storage_device"
const val STORAGE_FOLDER = "storage_folder"
const val STORAGE_VIDEO = "storage_video"

const val KEY_MEDIA_URI = "media_uri"
const val KEY_DURATION = "media_duration"

const val EXTRA_MEDIAID = "org.opensilk.extra.mediaid"

object NoMediaId: MediaId {
    override val json: String = ""
}

object NoMediaRef: MediaRef {
    override val id: MediaId = NoMediaId
}

/**
 * A generic media identity interface
 *
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
 * Parse this json string to a [MediaId]
 */
fun String.toMediaId(): MediaId = parseMediaId(this)

/**
 * Place mediaId into Intent
 */
fun Intent.putMediaIdExtra(mediaId: MediaId) = putExtra(EXTRA_MEDIAID, mediaId.json)

/**
 * Retrieve mediaId from Intent
 */
fun Intent.getMediaIdExtra() = parseMediaId(getStringExtra(EXTRA_MEDIAID))

/**
 * Place mediaId into Bundle
 */
fun Bundle.putMediaId(mediaId: MediaId) = putString(EXTRA_MEDIAID, mediaId.json)

/**
 * Retrieve mediaId from Bundle
 */
fun Bundle.getMediaId() = parseMediaId(getString(EXTRA_MEDIAID))

/**
 * Place mediaId into Bundle
 */
fun PersistableBundle.putMediaId(mediaId: MediaId) = putString(EXTRA_MEDIAID, mediaId.json)

/**
 * Retrieve mediaId from Bundle
 */
fun PersistableBundle.getMediaId() = parseMediaId(getString(EXTRA_MEDIAID))

/**
 * Wrap the mediaId in a bundle
 */
fun MediaId.asBundle() = bundle(EXTRA_MEDIAID, this.json)

/**
 * Recovers a MediaId from its json representation
 */
internal fun parseMediaId(json: String): MediaId {
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
                DOCUMENT_DIRECTORY -> {
                    mediaId = DocDirectoryIdTransformer.read(jr, readVersion(jr))
                }
                DOCUMENT_VIDEO -> {
                    mediaId = DocVideoIdTransformer.read(jr, readVersion(jr))
                }
                STORAGE_DEVICE -> {
                    mediaId = StorageDeviceIdTransformer.read(jr, readVersion(jr))
                }
                STORAGE_FOLDER -> {
                    mediaId = StorageFolderIdTransformer.read(jr, readVersion(jr))
                }
                STORAGE_VIDEO -> {
                    mediaId = StorageVideoIdTransformer.read(jr, readVersion(jr))
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
        is UpnpDeviceRef -> {
            bob.setTitle(meta.title)
                    .setSubtitle(meta.subtitle)
                    .setIconUri(meta.artworkUri)
                    .setMediaId(id.json)
        }
        is UpnpFolderRef -> {
            bob.setTitle(meta.title)
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
        is DocTreeDeviceRef -> {
            bob.setTitle(meta.title)
                    .setMediaId(id.json)
        }
        is DocFileDeviceRef -> {
            bob.setTitle(meta.title)
                    .setMediaId(id.json)
        }
        is DocDirectoryRef -> {
            bob.setTitle(meta.title)
                    .setMediaId(id.json)
        }
        is StorageDeviceRef -> {
            bob.setTitle(meta.title)
                    .setMediaId(id.json)
        }
        is StorageFolderRef -> {
            bob.setTitle(meta.title)
                    .setMediaId(id.json)
        }
        //override this above if need special handling
        is VideoRef -> {
            bob.setTitle(meta.title)
                    .setSubtitle(meta.subtitle)
                    .setIconUri(meta.artworkUri)
                    .setMediaId(id.json)
                    .setExtras(bundle(KEY_MEDIA_URI, meta.mediaUri)
                            ._putLong(KEY_DURATION, meta.duration))
        }
        else -> TODO("Unsupported mediaRef $this")
    }
    return bob.build()
}

fun MediaRef.toMediaItem(): MediaItem = when (this) {
    is UpnpDeviceRef -> {
        MediaItem(toMediaDescription(), MediaItem.FLAG_BROWSABLE)
    }
    is UpnpFolderRef -> {
        MediaItem(toMediaDescription(), MediaItem.FLAG_BROWSABLE)
    }
    is UpnpMusicTrackRef -> {
        MediaItem(toMediaDescription(), MediaItem.FLAG_PLAYABLE)
    }
    is UpnpAudioRef -> {
        MediaItem(toMediaDescription(), MediaItem.FLAG_PLAYABLE)
    }
    is DocFileDeviceRef,
    is DocTreeDeviceRef -> {
        MediaItem(toMediaDescription(), MediaItem.FLAG_PLAYABLE)
    }
    is DocDirectoryRef -> {
        MediaItem(toMediaDescription(), MediaItem.FLAG_BROWSABLE)
    }
    is StorageDeviceRef -> {
        MediaItem(toMediaDescription(), MediaItem.FLAG_BROWSABLE)
    }
    is StorageFolderRef -> {
        MediaItem(toMediaDescription(), MediaItem.FLAG_BROWSABLE)
    }
    //override this above if need special handling
    is VideoRef -> {
        MediaItem(toMediaDescription(), MediaItem.FLAG_PLAYABLE)
    }
    else -> TODO("Unsupported mediaRef $this")
}
