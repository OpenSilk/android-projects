package org.opensilk.media

/**
 * Folder classification
 */
interface FolderId: MediaId

object NoFolderId: FolderId {
    override val json: String = ""
}

/**
 * Folder metadata
 */
interface FolderMeta {
    val title: String
}

interface FolderRef: MediaRef {
    override val id: FolderId
    val meta: FolderMeta
}