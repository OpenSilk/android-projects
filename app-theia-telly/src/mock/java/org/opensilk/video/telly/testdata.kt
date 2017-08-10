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
fun testUpnpDeviceMeta(): UpnpDeviceRef {
    return UpnpDeviceRef(
            id = UPNP_DEVICE_ID_1,
            meta = UpnpDeviceMeta(
                    "Mock CDSService",
                    "Made by Mock"
            )
    )
}

/**
 * a mock upnp folder media list
 */
fun testUpnpFolderMetas(): List<UpnpFolderRef> {
    val metaList = ArrayList<UpnpFolderRef>()
    for (ii in 1..50) {
        val ref = UpnpFolderRef(
                id = UpnpFolderId(
                        deviceId = UPNP_DEVICE_ID_1.deviceId,
                        folderId = "$ii"
                ),
                parentId = UpnpFolderId(
                        deviceId = UPNP_DEVICE_ID_1.deviceId,
                        folderId = "0"
                ),
                meta = UpnpFolderMeta(
                        title = "Folder ${ii.zeroPad(2)}"
                )
        )
        metaList.add(ref)
    }
    return metaList
}

/**
 * a mock video item list
 */
fun testUpnpVideoMetas(): List<UpnpVideoRef> {
    val metaList = ArrayList<UpnpVideoRef>()
    //first folder gets 10 items
    for (ii in 1..10) {
        metaList.add(makeUpnpVideoItem("1:$ii", "1", ii))
    }
    //rest get one item
    for (ii in 2..50) {
        metaList.add(makeUpnpVideoItem("$ii:1", "$ii", ii))
    }
    return metaList
}

fun makeUpnpVideoItem(id: String, parentId: String, idx: Int): UpnpVideoRef {
    return UpnpVideoRef(
            id = UpnpVideoId(
                    UPNP_DEVICE_ID_1.deviceId,
                    id
            ),
            parentId = UpnpFolderId(
                    UPNP_DEVICE_ID_1.deviceId,
                    parentId
            ),
            meta = UpnpVideoMeta(
                    title =  "JellyFish $id",
                    mimeType = "video/matroska",
                    mediaTitle = "jelly-fish-${idx.zeroPad(3)}-3-mbps-hd-h264.mkv",
                    mediaUri = Uri.parse("file:///android_asset/jellyfish-3-mbps-hd-h264.mkv"),
                    size = 1024 * 1024 * 10,
                    duration = 3030,
                    bitrate = 3_055_616,
                    resolution = "1920x1080"
            )
    )
}

