package org.opensilk.media

/**
 * Created by drew on 8/11/17.
 */
data class UpnpMusicGenreId(
        override val deviceId: String,
        override val containerId: String
): UpnpContainerId {
    override val json: String
        get() = writeJson(UpnpMusicGenreTransformer, this)
}

data class UpnpMusicGenreMeta(
        override val title: String = ""
): UpnpMeta

data class UpnpMusicGenreRef(
        override val id: UpnpMusicGenreId,
        override val parentId: UpnpContainerId,
        override val meta: UpnpMusicGenreMeta
): UpnpContainerRef

internal object UpnpMusicGenreTransformer: UpnpContainerTransformer() {
    override val kind: String = UPNP_MUSIC_GENRE
}