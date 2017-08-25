package org.opensilk.media

/**
 * Created by drew on 8/11/17.
 */
data class UpnpFolderId(
        override val deviceId: String,
        override val parentId: String,
        override val containerId: String
): UpnpContainerId, FolderId {

    override val json: String
        get() = writeJson(UpnpFolderTransformer, this)

}

data class UpnpFolderRef(
        override val id: UpnpFolderId,
        override val meta: UpnpFolderMeta
): UpnpContainerRef, FolderRef

data class UpnpFolderMeta(
        override val title: String
): UpnpMeta, FolderMeta

internal object UpnpFolderTransformer: UpnpContainerTransformer() {
    override val kind: String = UPNP_FOLDER
}