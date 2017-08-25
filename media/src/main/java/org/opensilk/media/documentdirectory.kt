package org.opensilk.media

import android.net.Uri
import android.provider.DocumentsContract

data class DocDirectoryId(
        override val treeUri: Uri,
        override val documentId: String = if (isTreeUri(treeUri))
            DocumentsContract.getTreeDocumentId(treeUri) else
            DocumentsContract.getDocumentId(treeUri),
        override val parentId: String = documentId
): DocumentId, FolderId {
    override val json: String by lazy {
        writeJson(DocDirectoryIdTransformer, this)
    }
}

data class DocDirectoryMeta(
        override val title: String,
        override val mimeType: String,
        override val lastMod: Long = 0,
        override val flags: Long
): DocumentMeta, FolderMeta

data class DocDirectoryRef(
        override val id: DocDirectoryId,
        override val meta: DocDirectoryMeta
): DocumentRef, FolderRef

internal object DocDirectoryIdTransformer : DocumentIdTransformer() {
    override val kind: String = DOCUMENT_DIRECTORY
}