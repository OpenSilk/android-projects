package org.opensilk.media.loader.cds

import android.net.Uri
import org.fourthline.cling.support.model.container.Container
import org.fourthline.cling.support.model.item.VideoItem
import org.opensilk.media.*
import java.util.concurrent.TimeUnit

/**
 * Transforms Container into UpnpFolderRef
 */
fun Container.toMediaMeta(deviceId: UpnpDeviceId): UpnpFolderRef {
    val mediaId = UpnpFolderId(deviceId.deviceId, this.id)
    val parentMediaId = UpnpFolderId(deviceId.deviceId, this.parentID)
    val title = this.title
    return UpnpFolderRef(
            mediaId,
            parentMediaId,
            UpnpFolderMeta(
                    title = title
            )
    )
}

/**
 * Transforms videoItem into UpnpVideoRef
 */
fun VideoItem.toMediaMeta(deviceId: UpnpDeviceId): UpnpVideoRef {
    val mediaId = UpnpVideoId(deviceId.deviceId, this.id)
    val parentMediaId = UpnpFolderId(deviceId.deviceId, this.parentID)
    val res = this.firstResource
    val mediaUri = Uri.parse(res.value)
    val mimeType = res.protocolInfo.contentFormat
    val duration = if (res.duration != null) parseUpnpDuration(res.duration) else 0
    val bitrate = res.bitrate ?: 0L
    val size = res.size ?: 0L
    val resolution = res.resolution ?: ""
    val displayName = this.title
    return UpnpVideoRef(
            id = mediaId,
            parentId = parentMediaId,
            meta = UpnpVideoMeta(
                    mediaTitle = displayName,
                    mimeType = mimeType,
                    duration = duration,
                    bitrate = bitrate,
                    size = size,
                    resolution = resolution,
                    mediaUri = mediaUri
            )
    )
}

/**
 * Parses upnp duration
 * returns 0 on error
 */
fun parseUpnpDuration(dur: String): Long {
    if (dur.isNullOrBlank()) {
        return 0L
    }
    val strings = dur.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
    if (strings.size != 3) {
        return 0L
    }
    try {
        var sec = 0L
        if (!strings[0].isNullOrEmpty()) {
            sec += TimeUnit.SECONDS.convert(Integer.decode(strings[0]).toLong(), TimeUnit.HOURS).toInt()
        }
        sec += TimeUnit.SECONDS.convert(Integer.decode(strings[1]).toLong(), TimeUnit.MINUTES).toInt()
        sec += TimeUnit.SECONDS.convert(Integer.decode(strings[2].substring(0, 2)).toLong(), TimeUnit.SECONDS).toInt()
        return sec
    } catch (e: NumberFormatException) {
        return 0L
    }

}