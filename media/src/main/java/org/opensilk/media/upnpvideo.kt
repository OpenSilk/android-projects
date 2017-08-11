package org.opensilk.media

import android.net.Uri

/**
 * Created by drew on 8/11/17.
 */
data class UpnpVideoId(
        override val deviceId: String,
        override val itemId: String): UpnpItemId {

    override val json: String by lazy {
        writeJson(UpnpVideoTransformer, this)
    }

}

data class UpnpVideoMeta(
        override val title: String = "",
        val subtitle: String = "",
        val artworkUri: Uri = Uri.EMPTY,
        val backdropUri: Uri = Uri.EMPTY,
        val mediaTitle: String,
        val mediaUri: Uri,
        val mimeType: String,
        val duration: Long = 0,
        val size: Long = 0,
        val bitrate: Long = 0,
        val resolution: String = ""): UpnpMeta

data class UpnpVideoRef(
        override val id: UpnpVideoId,
        override val parentId: UpnpContainerId,
        val tvEpisodeId: TvEpisodeId? = null,
        val movieId: MovieId? = null,
        override val meta: UpnpVideoMeta): UpnpItemRef

internal object UpnpVideoTransformer: UpnpItemTransformer() {
    override val kind: String = UPNP_VIDEO
}