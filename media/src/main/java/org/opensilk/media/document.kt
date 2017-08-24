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

/**
 * Created by drew on 8/11/17.
 */
data class DocumentId(val treeUri: Uri,
                      val documentId: String = if (isTreeUri(treeUri))
                          DocumentsContract.getTreeDocumentId(treeUri) else
                          DocumentsContract.getDocumentId(treeUri),
                      val parentId: String = documentId,
                      val mimeType: String): MediaId {

    val authority: String = treeUri.authority

    val isRoot: Boolean by lazy {
        if (isFromTree) {
            DocumentsContract.getTreeDocumentId(treeUri) == documentId
        } else {
            true
        }
    }

    val mediaUri: Uri by lazy {
        if (isFromTree) {
            DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId)
        } else {
            DocumentsContract.buildDocumentUri(authority, documentId)
        }
    }

    val childrenUri: Uri by lazy {
        if (isFromTree) {
            DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, documentId)
        } else {
            throw IllegalArgumentException("This documentId does not represent a tree")
        }
    }

    val isFromTree: Boolean by lazy {
        isTreeUri(treeUri)
    }

    val isDirectory: Boolean by lazy {
        mimeType == DocumentsContract.Document.MIME_TYPE_DIR
    }

    val isVideo: Boolean by lazy {
        mimeType.startsWith("video", true)
    }

    val isAudio: Boolean by lazy {
        mimeType.startsWith("audio", true)
                || mimeType.contains("flac", true)
                || mimeType.contains("ogg", true)
    }

    override val json: String by lazy {
        writeJson(DocumentIdTransformer, this)
    }

}

private fun isTreeUri(treeUri: Uri): Boolean {
    return if (Build.VERSION.SDK_INT >= 24) {
        DocumentsContract.isTreeUri(treeUri)
    } else {
        //this is the same as DocumentsContract.isTreeUri
        val paths = treeUri.pathSegments
        paths.size >= 2 && "tree" == paths[0]
    }
}

internal object DocumentIdTransformer: MediaIdTransformer<DocumentId> {

    override val kind: String = DOCUMENT
    override val version: Int = 1

    override fun write(jw: JsonWriter, item: DocumentId) {
        jw.name("tree").value(item.treeUri.toString())
        jw.name("doc").value(item.documentId)
        jw.name("par").value(item.parentId)
        jw.name("mime").value(item.mimeType)
    }

    override fun read(jr: JsonReader, version: Int): DocumentId {
        var treeStr = ""
        var docStr = ""
        var parent = ""
        var mime = ""
        while (jr.hasNext()) {
            when (jr.nextName()) {
                "tree" -> treeStr = jr.nextString()
                "doc" -> docStr = jr.nextString()
                "par" -> parent = jr.nextString()
                "mime" -> mime = jr.nextString()
                else -> jr.skipValue()
            }
        }
        return DocumentId(
                treeUri = Uri.parse(treeStr),
                documentId = docStr,
                parentId = parent,
                mimeType = mime)
    }

}