package org.opensilk.media

import android.util.JsonReader
import android.util.JsonWriter

/**
 * Created by drew on 8/11/17.
 */
interface UpnpMeta {
    val title: String
}

interface UpnpContainerId: MediaId {
    val deviceId: String
    val containerId: String
}

interface UpnpItemId: MediaId {
    val deviceId: String
    val itemId: String
}

interface UpnpContainerRef: MediaRef {
    val parentId: UpnpContainerId
    val meta: UpnpMeta
}

interface UpnpItemRef: MediaRef {
    val parentId: UpnpContainerId
    val meta: UpnpMeta
}

internal abstract class UpnpContainerTransformer: MediaIdTransformer<UpnpContainerId> {
    override val version: Int = 1

    override fun write(jw: JsonWriter, item: UpnpContainerId) {
        jw.name("dev").value(item.deviceId)
        jw.name("fol").value(item.containerId)
    }

    override fun read(jr: JsonReader): UpnpContainerId {
        var dev = ""
        var fol = ""
        while (jr.hasNext()) {
            when (jr.nextName()) {
                "dev" -> dev = jr.nextString()
                "fol" -> fol = jr.nextString()
                else -> jr.skipValue()
            }
        }
        return when (kind) {
            UPNP_FOLDER -> UpnpFolderId(dev, fol)
            else -> TODO()
        }
    }
}

internal abstract class UpnpItemTransformer: MediaIdTransformer<UpnpItemId> {
    override val version: Int = 1

    override fun write(jw: JsonWriter, item: UpnpItemId) {
        jw.name("dev").value(item.deviceId)
        jw.name("itm").value(item.itemId)
    }

    override fun read(jr: JsonReader): UpnpItemId {
        var dev = ""
        var itm = ""
        while (jr.hasNext()) {
            when (jr.nextName()) {
                "dev" -> dev = jr.nextString()
                "itm" -> itm = jr.nextString()
                else -> jr.skipValue()
            }
        }
        return when (kind) {
            UPNP_VIDEO -> UpnpVideoId(dev, itm)
            else -> TODO()
        }
    }
}

