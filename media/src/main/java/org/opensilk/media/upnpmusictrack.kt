package org.opensilk.media

import android.net.Uri

/**
 * Created by drew on 8/11/17.
 */
data class UpnpMusicTrackId(
        override val deviceId: String,
        override val itemId: String
): UpnpItemId {
    override val json: String
        get() = writeJson(UpnpMusicTrackTransformer, this)
}

data class UpnpMusicTrackMeta(
        override val title: String = "",
        val creator: String = "",
        val date: String = "",
        val artist: String = "",
        val album: String = "",
        val genre: String = "",
        val trackNum: Int = 0,
        override val size: Long = 0,
        override val duration: Long = 0,
        val bitrate: Long = 0,
        val nrAudioChan: Int = 0,
        val sampleFreq: Long = 0,
        override val mediaUri: Uri,
        override val mimeType: String,
        val originalArtworkUri: Uri = Uri.EMPTY,
        val artworkUri: Uri = Uri.EMPTY,
        val backdropUri: Uri = Uri.EMPTY,
        val originalTitle: String = ""
): UpnpItemMeta

data class UpnpMusicTrackRef(
        override val id: UpnpMusicTrackId,
        override val parentId: UpnpContainerId,
        override val meta: UpnpMusicTrackMeta
): UpnpItemRef

internal object UpnpMusicTrackTransformer: UpnpItemTransformer() {
    override val kind: String = UPNP_MUSIC_TRACK
}