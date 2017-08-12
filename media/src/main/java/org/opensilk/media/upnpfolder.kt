package org.opensilk.media

import android.net.Uri
import android.util.JsonReader
import android.util.JsonWriter

/**
 * Created by drew on 8/11/17.
 */
data class UpnpFolderId(
        override val deviceId: String,
        override val containerId: String
): UpnpContainerId {

    @Deprecated(replaceWith = ReplaceWith("containerId"), message = "Use containerId")
    val folderId: String = containerId

    override val json: String
        get() = writeJson(UpnpFolderTransformer, this)

}

data class UpnpFolderRef(
        override val id: UpnpFolderId,
        override val parentId: UpnpContainerId,
        override val meta: UpnpFolderMeta
): UpnpContainerRef

data class UpnpFolderMeta(
        override val title: String,
        val artworkUri: Uri = Uri.EMPTY
): UpnpMeta

internal object UpnpFolderTransformer: UpnpContainerTransformer() {
    override val kind: String = UPNP_FOLDER
}