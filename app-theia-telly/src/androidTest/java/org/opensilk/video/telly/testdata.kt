package org.opensilk.video.telly

import android.media.MediaDescription
import android.media.browse.MediaBrowser
import org.opensilk.media.MediaMeta
import org.opensilk.media._setMediaMeta
import rx.Observable

/**
 * Created by drew on 6/1/17.
 */
fun getTestUpnpDeviceItem(): MediaBrowser.MediaItem {
    val id = "mockupnpservice-1"
    val label = "Mock CDService"
    val subtitle = "Made by mock"
    val mediaExtras = MediaMeta()
    val builder = MediaDescription.Builder()
            .setTitle(label)
            .setSubtitle(subtitle)
            ._mediaRef(MediaRef(UPNP_DEVICE, id))
            ._setMediaMeta(mediaExtras)
    return MediaBrowser.MediaItem(builder.build(), MediaBrowser.MediaItem.FLAG_BROWSABLE)
}