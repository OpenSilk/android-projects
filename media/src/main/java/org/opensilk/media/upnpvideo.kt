package org.opensilk.media

import android.net.Uri

/**
 * Created by drew on 8/11/17.
 */
data class UpnpVideoId(
        override val deviceId: String,
        override val parentId: String,
        override val itemId: String
): UpnpItemId {
    override val json: String
        get() = writeJson(UpnpVideoTransformer, this)
}

data class UpnpVideoMeta(
        override val title: String = "",
        val subtitle: String = "",
        val artworkUri: Uri = Uri.EMPTY,
        val backdropUri: Uri = Uri.EMPTY,
        val originalTitle: String = "",
        override val mediaUri: Uri,
        override val mimeType: String,
        override val duration: Long = 0,
        override val size: Long = 0,
        val bitrate: Long = 0,
        val resolution: String = "",
        val sampleFreq: Long = 0,
        val nrAudioChan: Int = 0
): UpnpItemMeta

data class UpnpVideoRef(
        override val id: UpnpVideoId,
        val tvEpisodeId: TvEpisodeId? = null,
        val movieId: MovieId? = null,
        override val meta: UpnpVideoMeta
): UpnpItemRef

internal object UpnpVideoTransformer: UpnpItemTransformer() {
    override val kind: String = UPNP_VIDEO
}