package org.opensilk.media.loader.cds

import android.net.Uri
import org.fourthline.cling.support.model.DIDLObject
import org.fourthline.cling.support.model.container.MusicAlbum
import org.fourthline.cling.support.model.container.MusicArtist
import org.fourthline.cling.support.model.container.MusicGenre
import org.fourthline.cling.support.model.container.StorageFolder
import org.fourthline.cling.support.model.item.AudioItem
import org.fourthline.cling.support.model.item.MusicTrack
import org.fourthline.cling.support.model.item.VideoItem
import org.opensilk.media.*
import java.util.concurrent.TimeUnit

/**
 * Transforms Container into UpnpFolderRef
 */
fun StorageFolder.toUpnpFolder(parentId: UpnpContainerId): UpnpFolderRef {
    val mediaId = UpnpFolderId(parentId.deviceId, this.id)
    val title = this.title
    return UpnpFolderRef(
            id =  mediaId,
            parentId = parentId,
            meta = UpnpFolderMeta(
                    title = title
            )
    )
}

fun MusicAlbum.toUpnpMusicAlbum(parentId: UpnpContainerId): UpnpMusicAlbumRef {
    val mediaId = UpnpMusicAlbumId(parentId.deviceId, this.id)
    val title = this.title
    val creator = this.creator ?: ""
    val artist = this.firstArtist?.name ?: ""
    val genre = this.firstGenre ?: ""
    val artwork = if (this.hasProperty(DIDLObject.Property.UPNP.ALBUM_ART_URI::class.java)) {
        Uri.parse(this.getFirstProperty(DIDLObject.Property.UPNP.ALBUM_ART_URI::class.java).value.toString())
    } else Uri.EMPTY
    return UpnpMusicAlbumRef(
            id = mediaId,
            parentId = parentId,
            meta = UpnpMusicAlbumMeta(
                    title = title,
                    creator = creator,
                    artist = artist,
                    genre = genre,
                    originalArtworkUri = artwork
            )
    )
}

fun MusicGenre.toUpnpMusicGenre(parentId: UpnpContainerId): UpnpMusicGenreRef {
    val mediaId = UpnpMusicGenreId(parentId.deviceId, this.id)
    val title = this.title
    return UpnpMusicGenreRef(
            mediaId,
            parentId,
            UpnpMusicGenreMeta(
                    title = title
            )
    )
}

fun MusicArtist.toUpnpMusicArtist(parentId: UpnpContainerId): UpnpMusicArtistRef {
    val mediaId = UpnpMusicArtistId(parentId.deviceId, this.id)
    val title = this.title
    val genre = this.firstGenre ?: ""
    return UpnpMusicArtistRef(
            id = mediaId,
            parentId = parentId,
            meta = UpnpMusicArtistMeta(
                    title = title,
                    genre = genre
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
    val title = this.title
    val nrChan = res.nrAudioChannels?.toInt() ?: 0
    val sampleF = res.sampleFrequency ?: 0L
    return UpnpVideoRef(
            id = mediaId,
            parentId = parentMediaId,
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

fun AudioItem.toUpnpAudioTrack(parentId: UpnpContainerId): UpnpAudioRef {
    val mediaId = UpnpAudioId(parentId.deviceId, this.id)
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
            parentId = parentId,
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

fun MusicTrack.toUpnpMusicTrack(parentId: UpnpContainerId): UpnpMusicTrackRef {
    val mediaId = UpnpMusicTrackId(parentId.deviceId, this.id)
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
            parentId = parentId,
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