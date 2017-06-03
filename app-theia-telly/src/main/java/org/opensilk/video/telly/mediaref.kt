package org.opensilk.video.telly

import android.media.MediaDescription
import android.provider.MediaStore
import android.util.JsonReader
import android.util.JsonWriter
import java.io.StringReader
import java.io.StringWriter

const val UPNP_DEVICE = "upnp_device"
const val UPNP_FOLDER = "upnp_folder"
const val UPNP_VIDEO = "upnp_video"

/**
 * Created by drew on 5/29/17.
 */
interface MediaId {
    val id: String
}

data class StringId(override val id: String): MediaId

data class FolderId(val deviceId: String, val folderId: String): MediaId {
    override val id: String by lazy {
        val sr = StringWriter()
        return@lazy JsonWriter(sr).use { jr ->
            jr.beginObject()
            jr.name("dev").value(deviceId)
            jr.name("fol").value(folderId)
            jr.endObject()
            return@use sr.toString()
        }
    }
}

fun newFolderId(json: String) : FolderId {
    var dev = ""
    var fol = ""
    return JsonReader(StringReader(json)).use { jr ->
        jr.beginObject()
        while (jr.hasNext()) {
            when (jr.nextName()) {
                "dev" -> dev = jr.nextString()
                "fol" -> fol = jr.nextString()
                else -> jr.skipValue()
            }
        }
        jr.endObject()
        return@use FolderId(dev, fol)
    }
}

data class UpnpItemId(val deviceId: String, val itemId: String): MediaId {
    override val id: String by lazy {
        val sr = StringWriter()
        return@lazy JsonWriter(sr).use { jr ->
            jr.beginObject()
            jr.name("dev").value(deviceId)
            jr.name("itm").value(itemId)
            jr.endObject()
            return@use sr.toString()
        }
    }
}

fun newUpnpItemId(json: String) : UpnpItemId {
    var dev = ""
    var itm = ""
    return JsonReader(StringReader(json)).use { jr ->
        jr.beginObject()
        while (jr.hasNext()) {
            when (jr.nextName()) {
                "dev" -> dev = jr.nextString()
                "itm" -> itm = jr.nextString()
                else -> jr.skipValue()
            }
        }
        jr.endObject()
        return@use UpnpItemId(dev, itm)
    }
}

data class MediaRef(val kind: String, val mediaId: MediaId) {

    constructor(kind: String, id: String) : this(kind, StringId(id))

    fun toJson(): String {
        return StringWriter().use {
            val jw = JsonWriter(it)
            jw.beginObject()
            jw.name("ver").value(1)
            jw.name("kind").value(kind)
            jw.name("id").value(mediaId.id)
            jw.endObject()
            return@use it.toString()
        }
    }
}

fun newMediaRef(json: String): MediaRef {
    var kind = ""
    var id = ""
    return JsonReader(StringReader(json)).use { jr ->
        jr.beginObject()
        while (jr.hasNext()) {
            when (jr.nextName()) {
                "ver" -> jr.skipValue()
                "kind" -> kind = jr.nextString()
                "id" -> id = jr.nextString()
                else -> jr.skipValue()
            }
        }
        jr.endObject()
        when (kind) {
            UPNP_DEVICE -> {
                return@use MediaRef(kind, id)
            }
            UPNP_FOLDER -> {
                return@use MediaRef(kind, newFolderId(id))
            }
            else -> {
                TODO("Unknown mediaRef kind=$kind")
            }
        }
    }
}

fun MediaDescription.Builder._mediaRef(mediaRef: MediaRef) : MediaDescription.Builder {
    return this.setMediaId(mediaRef.toJson())
}