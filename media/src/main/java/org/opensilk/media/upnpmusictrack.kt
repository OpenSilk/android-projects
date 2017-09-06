package org.opensilk.media

import android.net.Uri

/**
 * Created by drew on 8/11/17.
 */
data class UpnpMusicTrackId(
        override val deviceId: String,
        override val parentId: String,
        override val itemId: String
): UpnpItemId, MusicId {
    override val json: String
        get() = writeJson(UpnpMusicTrackTransformer, this)
}

data class UpnpMusicTrackMeta(
        override val title: String = "",
        val creator: String = "",
        val date: String = "",
        override val artist: String = "",
        override val album: String = "",
        override val genre: String = "",
        override val trackNum: Int = 0,
        override val size: Long = 0,
        override val duration: Long = 0,
        override val bitrate: Long = 0,
        val nrAudioChan: Int = 0,
        val sampleFreq: Long = 0,
        override val mediaUri: Uri,
        override val mimeType: String,
        override val originalArtworkUri: Uri = Uri.EMPTY,
        override val artworkUri: Uri = Uri.EMPTY,
        override val backdropUri: Uri = Uri.EMPTY,
        override val originalTitle: String = "",
        override val albumArtist: String = "",
        override val discNumber: Int = 1,
        override val isCompilation: Boolean = false
): UpnpItemMeta, MusicMeta

data class UpnpMusicTrackRef(
        override val id: UpnpMusicTrackId,
        override val meta: UpnpMusicTrackMeta
): UpnpItemRef

internal object UpnpMusicTrackTransformer: UpnpItemTransformer() {
    override val kind: String = UPNP_MUSIC_TRACK
}