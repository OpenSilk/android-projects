package org.opensilk.media

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
    fun write(jw: JsonWriter)
}

data class UpnpDeviceId(val deviceId: String): MediaId {
    override fun write(jw: JsonWriter) {
        jw.name("dev").value(deviceId)
    }
}

internal fun newUpnpDeviceId(jr: JsonReader): UpnpDeviceId {
    var id = ""
    while (jr.hasNext()) {
        when (jr.nextName()) {
            "dev" -> id = jr.nextString()
            else -> jr.skipValue()
        }
    }
    return UpnpDeviceId(id)
}

data class UpnpFolderId(val deviceId: String, val folderId: String): MediaId {
    override fun write(jw: JsonWriter) {
        jw.name("dev").value(deviceId)
        jw.name("fol").value(folderId)
    }
}

internal fun newUpnpFolderId(jr: JsonReader) : UpnpFolderId {
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

data class UpnpVideoId(val deviceId: String, val itemId: String): MediaId {
    override fun write(jw: JsonWriter) {
        jw.name("dev").value(deviceId)
        jw.name("itm").value(itemId)
    }
}

internal fun newUpnpVideoId(jr: JsonReader) : UpnpVideoId {
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

data class MediaRef(val kind: String, val mediaId: MediaId) {

    fun toJson(): String {
        return StringWriter().use {
            val jw = JsonWriter(it)
            jw.beginObject()
            jw.name("ver").value(1)
            jw.name("kind").value(kind)
            jw.name("id")
            jw.beginObject()
            mediaId.write(jw)
            jw.endObject()
            jw.endObject()
            return@use it.toString()
        }
    }
}

fun newMediaRef(json: String): MediaRef {
    return JsonReader(StringReader(json)).use { jr ->
        var kind = ""
        var mediaId: MediaId? = null
        jr.beginObject()
        while (jr.hasNext()) {
            when (jr.nextName()) {
                "ver" -> jr.skipValue()
                "kind" -> kind = jr.nextString()
                "id" -> {
                    jr.beginObject()
                    when (kind) {
                        UPNP_DEVICE -> mediaId = newUpnpDeviceId(jr)
                        UPNP_FOLDER -> mediaId = newUpnpFolderId(jr)
                        UPNP_VIDEO -> mediaId = newUpnpVideoId(jr)
                        else -> TODO("unknown kind: $kind")
                    }
                    jr.endObject()
                }
                else -> jr.skipValue()
            }
        }
        jr.endObject()
        return@use MediaRef(kind, mediaId!!)
    }
}

/**
 *
 */
fun isLikelyJson(str: String): Boolean {
    return str.first() == '{' || str.first() == '['
}