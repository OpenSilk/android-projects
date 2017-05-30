package org.opensilk.video.telly

import android.media.MediaDescription
import android.util.JsonReader
import android.util.JsonWriter
import java.io.StringReader
import java.io.StringWriter

const val UPNP_DEVICE = "upnp_device"

/**
 * Created by drew on 5/29/17.
 */
data class MediaRef(val kind: String, val id: String) {
    fun toJson(): String {
        return StringWriter().use {
            val jw = JsonWriter(it)
            jw.beginObject()
            jw.name("ver").value(1)
            jw.name("kind").value(kind)
            jw.name("id").value(id)
            jw.endObject()
            return@use it.toString()
        }
    }
}

fun newMediaRef(json: String): MediaRef {
    val jr = JsonReader(StringReader(json))
    jr.beginObject()
    val ver = jr.nextInt()
    val kind = jr.nextString()
    val id = jr.nextString()
    jr.close()
    return MediaRef(kind, id)
}

fun MediaDescription.Builder._mediaRef(mediaRef: MediaRef) : MediaDescription.Builder {
    return this.setMediaId(mediaRef.toJson())
}