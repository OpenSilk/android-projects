package org.opensilk.video

import android.net.Uri
import android.provider.DocumentsContract
import org.fourthline.cling.model.meta.Device
import org.fourthline.cling.model.meta.RemoteDeviceIdentity
import org.fourthline.cling.support.model.container.Container
import org.fourthline.cling.support.model.item.VideoItem
import org.opensilk.media.*


fun Device<*,*,*>.toMediaMeta(): MediaMeta {
    val device = this
    val meta = MediaMeta()
    meta.mediaId = MediaRef(UPNP_DEVICE, UpnpDeviceId(device.identity.udn.identifierString)).toJson()
    meta.mimeType = MIME_TYPE_CONTENT_DIRECTORY
    meta.title = device.details.friendlyName ?: device.displayString
    meta.subtitle = if (device.displayString == meta.title) "" else device.displayString
    if (device.hasIcons()) {
        var largest = device.icons[0]
        for (ic in device.icons) {
            if (largest.height < ic.height) {
                largest = ic
            }
        }
        var uri = largest.uri.toString()
        //TODO fragile... only tested on MiniDLNA
        if (uri.startsWith("/")) {
            val ident = device.identity
            if (ident is RemoteDeviceIdentity) {
                val ru = ident.descriptorURL
                uri = "${ru.protocol}://${ru.host}:${ru.port}${uri}"
            }
        }
        meta.artworkUri = Uri.parse(uri)
    }
    return meta
}

fun Container.toMediaMeta(deviceId: UpnpDeviceId): MediaMeta {
    val meta = MediaMeta()

    meta.mediaId = MediaRef(UPNP_FOLDER, UpnpFolderId(deviceId.deviceId, this.id)).toJson()
    meta.parentMediaId = MediaRef(UPNP_FOLDER, UpnpFolderId(deviceId.deviceId, this.parentID)).toJson()
    meta.mimeType = DocumentsContract.Document.MIME_TYPE_DIR

    meta.title = this.title
    this.childCount?.let {
        meta.subtitle = "$it Children"
    }
    return meta
}

fun VideoItem.toMediaMeta(deviceId: UpnpDeviceId): MediaMeta {
    val meta = MediaMeta()
    meta.mediaId = MediaRef(UPNP_VIDEO, UpnpVideoId(deviceId.deviceId, this.id)).toJson()
    meta.parentMediaId = MediaRef(UPNP_FOLDER, UpnpFolderId(deviceId.deviceId, this.parentID)).toJson()
    val res = this.firstResource
    meta.mediaUri = Uri.parse(res.value)
    meta.mimeType = res.protocolInfo.contentFormat
    meta.duration = if (res.duration != null) parseUpnpDuration(res.duration) else 0
    meta.bitrate = res.bitrate ?: 0L
    meta.size = res.size ?: 0L
    meta.resolution = res.resolution ?: ""
    meta.displayName = this.title
    return meta
}



class NoContentDirectoryFoundException: Exception()
class NoBrowseResultsException: Exception()
class DeviceNotFoundException: Exception()
class ServiceNotFoundException: Exception()
