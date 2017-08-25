package org.opensilk.media

import android.util.JsonReader
import android.util.JsonWriter

/**
 * Created by drew on 8/22/17.
 */
data class StorageFolderId(
        override val path: String,
        override val uuid: String,
        val parent: String
): StorageContainerId {
    override val json: String
        get() = writeJson(StorageFolderIdTransformer, this)
}

data class StorageFolderMeta(
        override val title: String
): StorageMeta, FolderMeta

data class StorageFolderRef(
        override val id: StorageFolderId,
        override val meta: StorageFolderMeta
): StorageRef, FolderRef

internal object StorageFolderIdTransformer: MediaIdTransformer<StorageFolderId> {
    override val kind: String = STORAGE_FOLDER
    override val version: Int = 1

    override fun write(jw: JsonWriter, item: StorageFolderId) {
        jw.name("path").value(item.path)
        jw.name("uuid").value(item.uuid)
        jw.name("pare").value(item.parent)
    }

    override fun read(jr: JsonReader, version: Int): StorageFolderId {
        var path = ""
        var uuid = ""
        var pare = ""
        while (jr.hasNext()) {
            when (jr.nextName()) {
                "path" -> path = jr.nextString()
                "uuid" -> uuid = jr.nextString()
                "pare" -> pare = jr.nextString()
                else -> jr.skipValue()
            }
        }
        return StorageFolderId(
                uuid = uuid,
                path = path,
                parent = pare
        )
    }
}