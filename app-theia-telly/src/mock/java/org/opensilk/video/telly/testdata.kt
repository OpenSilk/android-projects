package org.opensilk.video.telly

import android.media.browse.MediaBrowser
import android.net.Uri
import org.opensilk.media.*

/**
 * a mock upnp device mediaItem
 */
fun testUpnpDeviceItem(): MediaBrowser.MediaItem {
    val mediaExtras = MediaMeta()
    mediaExtras.mediaId = MediaRef(UPNP_DEVICE, StringId("upnpcds-1")).toJson()
    mediaExtras.title = "Mock CDService"
    mediaExtras.subtitle = "Made by Mock"
    mediaExtras.mimeType = MIME_TYPE_CONTENT_DIRECTORY
    return mediaExtras.toMediaItem()
}

/**
 * a mock upnp folder mediaItem
 */
fun testUpnpFolderItem(): MediaBrowser.MediaItem {
    val mediaExtras = MediaMeta()
    mediaExtras.mediaId = MediaRef(UPNP_FOLDER, FolderId("upnpcds-1", "1")).toJson()
    mediaExtras.parentMediaId = FolderId("upnpcds-1", "0").id
    mediaExtras.title = "Mock Folder 1"
    mediaExtras.mimeType = MIME_TYPE_DIR
    return mediaExtras.toMediaItem()
}

/**
 * a mock list of upnp folder mediaItems
 */
fun testUpnpFolderItemList(): List<MediaBrowser.MediaItem> {
    val list = ArrayList<MediaBrowser.MediaItem>()
    for (ii in 1..10) {
        val mediaExtras = MediaMeta()
        mediaExtras.mediaId = MediaRef(UPNP_FOLDER, FolderId("upnpcds-1", "$ii")).toJson()
        mediaExtras.parentMediaId = FolderId("upnpcds-1", "0").id
        mediaExtras.title = "Mock Folder $ii"
        mediaExtras.mimeType = MIME_TYPE_DIR
        list.add(mediaExtras.toMediaItem())
    }
    return list
}

fun testUpnpVideoItem(): MediaBrowser.MediaItem {
    val mediaExtras = MediaMeta()
    mediaExtras.mediaId = MediaRef(UPNP_VIDEO, UpnpItemId("upnpcds-1", "video1")).toJson()
    mediaExtras.parentMediaId = MediaRef(UPNP_FOLDER, FolderId("upnpcds-1", "1")).toJson()
    mediaExtras.title = "Video 1"
    mediaExtras.subtitle = "Made by foo"
    mediaExtras.mimeType = "video/mp4"
    mediaExtras.mediaUri = Uri.parse("file:///android_asset/jellyfish-3-mbps-hd-h264.mkv")
    mediaExtras.size = 1024 * 1024 * 53
    return mediaExtras.toMediaItem()
}

