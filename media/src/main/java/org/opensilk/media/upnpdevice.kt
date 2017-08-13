package org.opensilk.media

import android.net.Uri
import android.util.JsonReader
import android.util.JsonWriter

/**
 * Created by drew on 8/11/17.
 */
data class UpnpDeviceId(override val deviceId: String): UpnpContainerId {

    override val parentId: String = UPNP_ROOT_ID
    override val containerId: String = UPNP_ROOT_ID

    override val json: String
        get() = writeJson(UpnpDeviceTransformer, this)

}

data class UpnpDeviceMeta(
        override val title: String,
        val subtitle: String = "",
        val artworkUri: Uri = Uri.EMPTY,
        val updateId: Long = 0): UpnpMeta

data class UpnpDeviceRef(
        override val id: UpnpDeviceId,
        override val meta: UpnpDeviceMeta): UpnpContainerRef

internal object UpnpDeviceTransformer: MediaIdTransformer<UpnpDeviceId> {
    override val kind: String = UPNP_DEVICE
    override val version: Int = 1

    override fun write(jw: JsonWriter, item: UpnpDeviceId) {
        jw.name("dev").value(item.deviceId)
    }

    override fun read(jr: JsonReader, version: Int): UpnpDeviceId {
        var dev = ""
        while (jr.hasNext()) {
            when (jr.nextName()) {
                "dev" -> dev = jr.nextString()
                else -> jr.skipValue()
            }
        }
        return UpnpDeviceId(dev)
    }
}