package org.opensilk.media

import android.net.Uri
import android.util.JsonReader
import android.util.JsonWriter

const val UPNP_ROOT_ID = "0"

/**
 * Created by drew on 8/11/17.
 *
 * Meta all upnp objects have
 */
interface UpnpMeta {
    val title: String
}

/**
 * Meta representing upnp item with resource
 */
interface UpnpItemMeta: UpnpMeta {
    val mediaUri: Uri
    val mimeType: String
    val duration: Long
    val size: Long
}

/**
 * id representing upnp container
 */
interface UpnpContainerId: MediaId {
    val deviceId: String
    val parentId: String
    val containerId: String
}

/**
 * id representing upnp item
 */
interface UpnpItemId: MediaId {
    val deviceId: String
    val parentId: String
    val itemId: String
}

/**
 * ref representing upnp container
 */
interface UpnpContainerRef: MediaRef {
    val meta: UpnpMeta
}

/**
 * ref representing upnp item
 */
interface UpnpItemRef: MediaRef {
    val meta: UpnpMeta
}

/**
 * Common transformer for upnp containers
 */
internal abstract class UpnpContainerTransformer: MediaIdTransformer<UpnpContainerId> {
    override val version: Int = 1

    override fun write(jw: JsonWriter, item: UpnpContainerId) {
        jw.name("dev").value(item.deviceId)
        jw.name("par").value(item.parentId)
        jw.name("fol").value(item.containerId)
    }

    override fun read(jr: JsonReader, version: Int): UpnpContainerId {
        var dev = ""
        var fol = ""
        var par = ""
        while (jr.hasNext()) {
            when (jr.nextName()) {
                "dev" -> dev = jr.nextString()
                "par" -> par = jr.nextString()
                "fol" -> fol = jr.nextString()
                else -> jr.skipValue()
            }
        }
        return when (kind) {
            UPNP_FOLDER -> UpnpFolderId(dev, par, fol)
            else -> TODO()
        }
    }
}

/**
 * Common transformer for upnp items
 */
internal abstract class UpnpItemTransformer: MediaIdTransformer<UpnpItemId> {
    override val version: Int = 1

    override fun write(jw: JsonWriter, item: UpnpItemId) {
        jw.name("dev").value(item.deviceId)
        jw.name("par").value(item.parentId)
        jw.name("itm").value(item.itemId)
    }

    override fun read(jr: JsonReader, version: Int): UpnpItemId {
        var dev = ""
        var itm = ""
        var par = ""
        while (jr.hasNext()) {
            when (jr.nextName()) {
                "dev" -> dev = jr.nextString()
                "par" -> par = jr.nextString()
                "itm" -> itm = jr.nextString()
                else -> jr.skipValue()
            }
        }
        return when (kind) {
            UPNP_AUDIO -> UpnpAudioId(dev, par, itm)
            UPNP_MUSIC_TRACK -> UpnpMusicTrackId(dev, par, itm)
            UPNP_VIDEO -> UpnpVideoId(dev, par, itm)
            else -> TODO()
        }
    }
}

