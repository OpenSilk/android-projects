package org.opensilk.media

/**
 * Created by drew on 8/22/17.
 */
interface FolderId: MediaContainerId

interface FolderMeta {
    val title: String
}

interface FolderRef: MediaRef {
    val meta: FolderMeta
}