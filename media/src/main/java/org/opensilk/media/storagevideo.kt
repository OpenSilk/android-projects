package org.opensilk.media

import android.util.JsonReader
import android.util.JsonWriter

/**
 * Created by drew on 8/22/17.
 */
data class StorageVideoId(
        val path: String,
        val uuid: String
): StorageId {
    override val json: String
        get() = writeJson(StorageVideoIdTransformer, this)
}

data class StorageVideoMeta(
        override val title: String,
        val size: Long,
        val lastMod: Long,
        val mimeType: String
): StorageMeta

data class StorageVideoRef(
        override val id: StorageVideoId,
        override val meta: StorageVideoMeta
): StorageRef

internal object StorageVideoIdTransformer: MediaIdTransformer<StorageVideoId> {
    override val kind: String = STORAGE_FOLDER
    override val version: Int = 1

    override fun write(jw: JsonWriter, item: StorageVideoId) {
        jw.name("path").value(item.path)
        jw.name("uuid").value(item.uuid)
    }

    override fun read(jr: JsonReader, version: Int): StorageVideoId {
        var path = ""
        var uuid = ""
        while (jr.hasNext()) {
            when (jr.nextName()) {
                "path" -> path = jr.nextString()
                "uuid" -> uuid = jr.nextString()
                else -> jr.skipValue()
            }
        }
        return StorageVideoId(
                uuid = uuid,
                path = path
        )
    }
}