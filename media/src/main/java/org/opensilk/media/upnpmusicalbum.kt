package org.opensilk.media

import android.net.Uri

/**
 * Created by drew on 8/11/17.
 */
data class UpnpMusicAlbumId(
        override val deviceId: String,
        override val containerId: String
): UpnpContainerId {
    override val json: String
        get() = writeJson(UpnpMusicAlbumTransformer, this)
}

data class UpnpMusicAlbumMeta(
        override val title: String = "",
        val creator: String = "",
        val genre: String = "",
        val artist: String = "",
        val originalArtworkUri: Uri = Uri.EMPTY,
        val artworkUri: Uri = Uri.EMPTY,
        val backdropUri: Uri = Uri.EMPTY
): UpnpMeta

data class UpnpMusicAlbumRef(
        override val id: UpnpMusicAlbumId,
        override val parentId: UpnpContainerId,
        override val meta: UpnpMusicAlbumMeta
): UpnpContainerRef

internal object UpnpMusicAlbumTransformer: UpnpContainerTransformer() {
    override val kind: String = UPNP_MUSIC_ALBUM
}