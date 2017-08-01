package org.opensilk.video.telly

import android.net.Uri
import org.opensilk.media.*
import org.opensilk.video.DatabaseClient
import org.opensilk.video.zeroPad

fun insertTestData(client: DatabaseClient) {
    client.addUpnpDevice(testUpnpDeviceMeta())
    testUpnpFolderMetas().forEach {
        client.addUpnpFolder(it)
    }
    testUpnpVideoMetas().forEach {
        client.addUpnpVideo(it)
    }
}

val UPNP_DEVICE_ID_1 = UpnpDeviceId("upnpcds-1")

/**
 * a mock upnp device mediaItem
 */
fun testUpnpDeviceMeta(): MediaMeta {
    val mediaExtras = MediaMeta()
    mediaExtras.mediaId = MediaRef(UPNP_DEVICE, UPNP_DEVICE_ID_1).toJson()
    mediaExtras.title = "Mock CDService"
    mediaExtras.subtitle = "Made by Mock"
    mediaExtras.mimeType = MIME_TYPE_CONTENT_DIRECTORY
    return mediaExtras
}

/**
 * a mock upnp folder media list
 */
fun testUpnpFolderMetas(): List<MediaMeta> {
    val metaList = ArrayList<MediaMeta>()
    for (ii in 1..50) {
        val mediaExtras = MediaMeta()
        mediaExtras.mediaId = MediaRef(UPNP_FOLDER, UpnpFolderId(UPNP_DEVICE_ID_1.deviceId, ii.toString())).toJson()
        mediaExtras.parentMediaId = MediaRef(UPNP_FOLDER, UpnpFolderId(UPNP_DEVICE_ID_1.deviceId, "0")).toJson()
        mediaExtras.title = "Folder ${ii.zeroPad(2)}"
        mediaExtras.mimeType = MIME_TYPE_DIR
        metaList.add(mediaExtras)
    }
    return metaList
}

/**
 * a mock video item list
 */
fun testUpnpVideoMetas(): List<MediaMeta> {
    val metaList = ArrayList<MediaMeta>()
    //first folder gets 10 items
    for (ii in 1..10) {
        metaList.add(makeUpnpVideoItem("1:$ii", "1"))
    }
    //rest get one item
    for (ii in 2..50) {
        metaList.add(makeUpnpVideoItem("$ii:1", "$ii"))
    }
    return metaList
}

fun makeUpnpVideoItem(id: String, parentId: String): MediaMeta {
    val mediaExtras = MediaMeta()
    mediaExtras.mediaId = MediaRef(UPNP_VIDEO, UpnpVideoId(UPNP_DEVICE_ID_1.deviceId, id)).toJson()
    mediaExtras.parentMediaId = MediaRef(UPNP_FOLDER, UpnpFolderId(UPNP_DEVICE_ID_1.deviceId, parentId)).toJson()
    mediaExtras.title = "JellyFish $id"
    mediaExtras.mimeType = "video/mkv"
    mediaExtras.displayName = "jellyfish-3-mbps-hd-h264.mkv"
    mediaExtras.mediaUri = Uri.parse("file:///android_asset/jellyfish-3-mbps-hd-h264.mkv")
    mediaExtras.size = 1024 * 1024 * 10
    mediaExtras.duration = 3030
    mediaExtras.bitrate = 3_055_616
    mediaExtras.resolution = "1920x1080"
    return mediaExtras
}

