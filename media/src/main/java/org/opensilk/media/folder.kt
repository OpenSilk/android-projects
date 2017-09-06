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
interface FolderMeta: MediaMeta

interface FolderRef: MediaRef {
    override val id: FolderId
    override val meta: FolderMeta
}