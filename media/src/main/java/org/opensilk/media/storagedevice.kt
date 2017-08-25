package org.opensilk.media

import android.util.JsonReader
import android.util.JsonWriter

/**
 * Created by drew on 8/22/17.
 */
data class StorageDeviceId(
        override val uuid: String,
        override val path: String,
        val isPrimary: Boolean
): StorageContainerId, MediaDeviceId {
    override val json: String
        get() = writeJson(StorageDeviceIdTransformer, this)
}

data class StorageDeviceMeta(
        override val title: String
): StorageMeta, MediaDeviceMeta

data class StorageDeviceRef(
        override val id: StorageDeviceId,
        override val meta: StorageDeviceMeta
): StorageRef, MediaDeviceRef

internal object StorageDeviceIdTransformer: MediaIdTransformer<StorageDeviceId> {
    override val kind: String = STORAGE_DEVICE
    override val version: Int = 1

    override fun write(jw: JsonWriter, item: StorageDeviceId) {
        jw.name("uuid").value(item.uuid)
        jw.name("path").value(item.path)
        jw.name("prim").value(item.isPrimary)
    }

    override fun read(jr: JsonReader, version: Int): StorageDeviceId {
        var uuid = ""
        var path = ""
        var prim = false
        while (jr.hasNext()) {
            when (jr.nextName()) {
                "uuid" -> uuid = jr.nextString()
                "path" -> path = jr.nextString()
                "prim" -> prim = jr.nextBoolean()
                else -> jr.skipValue()
            }
        }
        return StorageDeviceId(
                uuid = uuid,
                path = path,
                isPrimary = prim
        )
    }
}