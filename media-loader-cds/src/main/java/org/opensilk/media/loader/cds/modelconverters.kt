package org.opensilk.media.loader.cds

import android.net.Uri
import org.fourthline.cling.support.model.DIDLObject
import org.fourthline.cling.support.model.container.*
import org.fourthline.cling.support.model.item.AudioItem
import org.fourthline.cling.support.model.item.MusicTrack
import org.fourthline.cling.support.model.item.VideoItem
import org.opensilk.media.*
import java.util.concurrent.TimeUnit

/**
 * Transforms Container into UpnpFolderRef
 */
fun Container.toUpnpFolder(deviceId: UpnpDeviceId): UpnpFolderRef {
    val mediaId = UpnpFolderId(deviceId = deviceId.deviceId, parentId = this.parentID, containerId = this.id)
    val title = this.title
    return UpnpFolderRef(
            id =  mediaId,
            meta = UpnpFolderMeta(
                    title = title
            )
    )
}

/**
 * Transforms videoItem into UpnpVideoRef
 */
fun VideoItem.toMediaMeta(deviceId: UpnpDeviceId): UpnpVideoRef {
    val mediaId = UpnpVideoId(deviceId = deviceId.deviceId, parentId = this.parentID, itemId = this.id)
    val res = this.firstResource
    val mediaUri = Uri.parse(res.value)
    val mimeType = res.protocolInfo.contentFormat
    val duration = if (res.duration != null) parseUpnpDuration(res.duration) else 0
    val bitrate = res.bitrate ?: 0L
    val size = res.size ?: 0L
    val resolution = res.resolution ?: ""
    val title = this.title
    val nrChan = res.nrAudioChannels?.toInt() ?: 0
    val sampleF = res.sampleFrequency ?: 0L
    return UpnpVideoRef(
            id = mediaId,
            meta = UpnpVideoMeta(
                    title = title,
                    mimeType = mimeType,
                    duration = duration,
                    bitrate = bitrate,
                    size = size,
                    resolution = resolution,
                    mediaUri = mediaUri,
                    nrAudioChan = nrChan,
                    sampleFreq = sampleF
            )
    )
}

fun AudioItem.toUpnpAudioTrack(deviceId: UpnpDeviceId): UpnpAudioRef {
    val mediaId = UpnpAudioId(deviceId = deviceId.deviceId, parentId = this.parentID, itemId = this.id)
    val title = this.title
    val creator = this.creator ?: ""
    val genre = this.firstGenre ?: ""
    val res = this.firstResource
    val mediaUri = Uri.parse(res.value)
    val mimeType = res.protocolInfo.contentFormat
    val duration = if (res.duration != null) parseUpnpDuration(res.duration) else 0
    val bitrate = res.bitrate ?: 0L
    val size = res.size ?: 0L
    val nrChan = res.nrAudioChannels?.toInt() ?: 0
    val sampleF = res.sampleFrequency ?: 0L
    return UpnpAudioRef(
            id = mediaId,
            meta = UpnpAudioMeta(
                    title = title,
                    creator = creator,
                    genre = genre,
                    mediaUri = mediaUri,
                    mimeType = mimeType,
                    duration = duration,
                    bitrate = bitrate,
                    size = size,
                    nrAudioChan = nrChan,
                    sampleFreq = sampleF
            )
    )
}

fun MusicTrack.toUpnpMusicTrack(deviceId: UpnpDeviceId): UpnpMusicTrackRef {
    val mediaId = UpnpMusicTrackId(deviceId = deviceId.deviceId, parentId = this.parentID, itemId = this.id)
    val title = this.title
    val creator = this.creator ?: ""
    val artist = this.firstArtist?.name ?: ""
    val res = this.firstResource
    val mediaUri = Uri.parse(res.value)
    val mimeType = res.protocolInfo.contentFormat
    val duration = if (res.duration != null) parseUpnpDuration(res.duration) else 0
    val bitrate = res.bitrate ?: 0L
    val size = res.size ?: 0L
    val nrChan = res.nrAudioChannels?.toInt() ?: 0
    val sampleF = res.sampleFrequency ?: 0L
    val artwork = if (this.hasProperty(DIDLObject.Property.UPNP.ALBUM_ART_URI::class.java)) {
        Uri.parse(this.getFirstProperty(DIDLObject.Property.UPNP.ALBUM_ART_URI::class.java).value.toString())
    } else Uri.EMPTY
    val trackNum = originalTrackNumber ?: 0
    val genre = firstGenre ?: ""
    val date = date ?: ""
    val album = album ?: ""
    return UpnpMusicTrackRef(
            id = mediaId,
            meta = UpnpMusicTrackMeta(
                    title = title,
                    creator = creator,
                    artist = artist,
                    mediaUri = mediaUri,
                    mimeType = mimeType,
                    duration = duration,
                    bitrate = bitrate,
                    size = size,
                    nrAudioChan = nrChan,
                    sampleFreq = sampleF,
                    originalArtworkUri = artwork,
                    trackNum = trackNum,
                    genre = genre,
                    date = date,
                    album = album
            )
    )
}

/**
 * Parses upnp duration to millis
 * returns 0 on error
 */
fun parseUpnpDuration(dur: String): Long {
    if (dur.isNullOrBlank()) {
        return 0L
    }
    val strings =  dur.split(":".toRegex()).toTypedArray()
    if (strings.size != 3) {
        return 0L
    }
    try {
        var sec = 0L
        if (!strings[0].isNullOrEmpty()) {
            sec += TimeUnit.MILLISECONDS.convert(strings[0].toLong(), TimeUnit.HOURS)
        }
        sec += TimeUnit.MILLISECONDS.convert(strings[1].toLong(), TimeUnit.MINUTES)
        sec += TimeUnit.MILLISECONDS.convert(strings[2].substring(0, 2).toLong(), TimeUnit.SECONDS)
        if (strings[2].length > 3 && !strings[2].contains("/")) {
            sec += TimeUnit.MILLISECONDS.convert(strings[2].substring(3).toLong(), TimeUnit.MILLISECONDS)
        }
        return sec
    } catch (e: NumberFormatException) {
        return 0L
    }

}