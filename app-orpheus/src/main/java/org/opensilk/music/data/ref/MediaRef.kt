/*
 * Copyright (c) 2016 OpenSilk Productions LLC
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.opensilk.music.data.ref

import android.net.Uri
import android.util.JsonReader
import android.util.JsonWriter
import java.io.StringReader
import java.io.StringWriter
import java.util.*

/**
 * Created by drew on 6/20/16.
 */
interface MediaRef {
    val mediaId: String
    val mediaUri: Uri
    val childrenUri: Uri
    companion object {
        fun parse(mediaId: String): MediaRef {
            val map = parseMediaId(mediaId)
            val kind = getKind(mediaId)
            return when(kind) {
                MEDIA_KIND_DOCUMENT -> DocumentRef(map)
                MEDIA_KIND_SPECIAL -> SpecialRef(map)
                else -> throw IllegalArgumentException("Unknown MediaRef kind=$kind")
            }
        }
        fun extractKind(mediaId: String): String {
            return getKind(mediaId)
        }
    }
}

const val MEDIA_KIND_DOCUMENT = "document"
const val MEDIA_KIND_SPECIAL = "special"

internal fun parseMediaId(mediaId: String): Map<String, String> {
    val sr = StringReader(getJson(mediaId))
    val jsonReader = JsonReader(sr)
    val map = HashMap<String, String>()
    try {
        jsonReader.beginObject()
        while (jsonReader.hasNext()) {
            map.put(jsonReader.nextName(), jsonReader.nextString())
        }
        jsonReader.endObject()
        return map
    } finally {
        jsonReader.close()
    }
}

internal fun buildMediaId(kind: String, values: Map<String, String>): String {
    val sw = StringWriter()
    val jsonWriter = JsonWriter(sw)
    try {
        jsonWriter.beginObject()
        for ((k, v) in values) {
            jsonWriter.name(k).value(v)
        }
        jsonWriter.endObject()
        return String.format(Locale.US, "%s\u2605%s", kind, sw.toString())
    } finally {
        jsonWriter.close()
    }
}

internal fun getJson(mediaId: String): String {
    return mediaId.substring(mediaId.indexOf("\u2605") + 1)
}

internal fun getKind(mediaId: String): String {
    return mediaId.substring(0, mediaId.indexOf("\u2605"))
}