package org.opensilk.media

import android.util.JsonReader
import android.util.JsonWriter

/**
 * Created by drew on 8/22/17.
 */
data class StorageFolderId(
        val path: String,
        val uuid: String
): StorageId {
    override val json: String
        get() = writeJson(StorageFolderIdTransformer, this)
}

data class StorageFolderMeta(
        override val title: String
): StorageMeta, FolderMeta

data class StorageDirectoryRef(
        override val id: StorageFolderId,
        override val meta: StorageFolderMeta
): StorageRef, FolderRef

internal object StorageFolderIdTransformer: MediaIdTransformer<StorageFolderId> {
    override val kind: String = STORAGE_FOLDER
    override val version: Int = 1

    override fun write(jw: JsonWriter, item: StorageFolderId) {
        jw.name("path").value(item.path)
        jw.name("uuid").value(item.uuid)
    }

    override fun read(jr: JsonReader, version: Int): StorageFolderId {
        var path = ""
        var uuid = ""
        while (jr.hasNext()) {
            when (jr.nextName()) {
                "path" -> path = jr.nextString()
                "uuid" -> uuid = jr.nextString()
                else -> jr.skipValue()
            }
        }
        return StorageFolderId(
                uuid = uuid,
                path = path
        )
    }
}