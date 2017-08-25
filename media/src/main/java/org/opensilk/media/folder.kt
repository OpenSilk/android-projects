package org.opensilk.media

/**
 * Folder classification
 */
interface FolderId: MediaId

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