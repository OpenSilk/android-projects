package org.opensilk.media

/**
 * Created by drew on 8/22/17.
 */
data class DirectoryDocumentMeta(
        override val title: String,
        override val mimeType: String,
        override val lastMod: Long = 0,
        override val flags: Long
): DocumentMeta, FolderMeta

data class DirectoryDocumentRef(
        override val id: DocumentId,
        override val meta: DirectoryDocumentMeta
): DocumentRef, FolderRef