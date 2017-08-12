package org.opensilk.media

import android.net.Uri

/**
 * Created by drew on 8/11/17.
 */
data class UpnpAudioId(
        override val deviceId: String,
        override val itemId: String
): UpnpItemId {
    override val json: String
        get() = writeJson(UpnpAudioTransformer, this)
}

data class UpnpAudioMeta(
        override val title: String = "",
        val creator: String = "",
        val genre: String = "",
        override val duration: Long = 0,
        override val size: Long = 0,
        override val mediaUri: Uri,
        override val mimeType: String,
        val bitrate: Long = 0,
        val nrAudioChan: Int = 0,
        val sampleFreq: Long = 0
): UpnpItemMeta

data class UpnpAudioRef(
        override val id: UpnpAudioId,
        override val parentId: UpnpContainerId,
        override val meta: UpnpAudioMeta
): UpnpItemRef

internal object UpnpAudioTransformer: UpnpItemTransformer() {
    override val kind: String = UPNP_AUDIO
}