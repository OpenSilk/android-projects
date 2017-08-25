package org.opensilk.media

import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.util.JsonReader
import android.util.JsonWriter

const val ROOTS_PARENT_ID = "\u2605G\u2605O\u2605D\u2605"

interface DocumentMeta {
    val title: String
    val mimeType: String
    val lastMod: Long
    val flags: Long
}

interface DocumentRef: MediaRef {
    override val id: DocumentId
    val meta: DocumentMeta
}

interface DocumentId: MediaId {
    val treeUri: Uri
    val documentId: String
    val parentId: String
}

val DocumentId.authority: String
    get() = treeUri.authority

val DocumentId.isRoot: Boolean
    get() = if (isTreeUri(treeUri)) {
        DocumentsContract.getTreeDocumentId(treeUri) == documentId
    } else {
        true
    }

val DocumentId.mediaUri: Uri
    get() = if (isTreeUri(treeUri)) {
        DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId)
    } else {
        DocumentsContract.buildDocumentUri(authority, documentId)
    }

val DocumentId.childrenUri: Uri
    get() = if (isTreeUri(treeUri)) {
        DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, documentId)
    } else {
        throw IllegalArgumentException("This documentId does not represent a tree")
    }

internal fun isTreeUri(treeUri: Uri): Boolean {
    return if (Build.VERSION.SDK_INT >= 24) {
        DocumentsContract.isTreeUri(treeUri)
    } else {
        //this is the same as DocumentsContract.isTreeUri
        val paths = treeUri.pathSegments
        paths.size >= 2 && "tree" == paths[0]
    }
}

internal abstract class DocumentIdTransformer: MediaIdTransformer<DocumentId> {

    override val version: Int = 1

    override fun write(jw: JsonWriter, item: DocumentId) {
        jw.name("tree").value(item.treeUri.toString())
        jw.name("doc").value(item.documentId)
        jw.name("par").value(item.parentId)
    }

    override fun read(jr: JsonReader, version: Int): DocumentId {
        var treeStr = ""
        var docStr = ""
        var parent = ""
        while (jr.hasNext()) {
            when (jr.nextName()) {
                "tree" -> treeStr = jr.nextString()
                "doc" -> docStr = jr.nextString()
                "par" -> parent = jr.nextString()
                else -> jr.skipValue()
            }
        }
        return when (kind) {
            DOCUMENT_DIRECTORY -> DirectoryDocumentId(
                    treeUri = Uri.parse(treeStr),
                    documentId = docStr,
                    parentId = parent)
            DOCUMENT_VIDEO -> VideoDocumentId(
                    treeUri = Uri.parse(treeStr),
                    documentId = docStr,
                    parentId = parent)
            else -> TODO()
        }
    }

}