package org.opensilk.media

/**
 * Created by drew on 8/11/17.
 */
data class UpnpMusicArtistId(
        override val deviceId: String,
        override val containerId: String
): UpnpContainerId {
    override val json: String
        get() = writeJson(UpnpMusicArtistTransformer, this)
}

data class UpnpMusicArtistMeta(
        override val title: String = "",
        val genre: String = ""
): UpnpMeta

data class UpnpMusicArtistRef(
        override val id: UpnpMusicArtistId,
        override val parentId: UpnpContainerId,
        override val meta: UpnpMusicArtistMeta
): UpnpContainerRef

internal object UpnpMusicArtistTransformer: UpnpContainerTransformer() {
    override val kind: String = UPNP_MUSIC_ARTIST
}