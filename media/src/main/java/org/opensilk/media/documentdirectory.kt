package org.opensilk.media

import android.net.Uri
import android.provider.DocumentsContract

data class DirectoryDocumentId(
        override val treeUri: Uri,
        override val documentId: String = if (isTreeUri(treeUri))
            DocumentsContract.getTreeDocumentId(treeUri) else
            DocumentsContract.getDocumentId(treeUri),
        override val parentId: String = documentId
): DocumentId, FolderId {
    override val json: String by lazy {
        writeJson(DirectoryDocumentIdTransformer, this)
    }
}

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

internal object DirectoryDocumentIdTransformer: DocumentIdTransformer() {
    override val kind: String = DOCUMENT_DIRECTORY
}