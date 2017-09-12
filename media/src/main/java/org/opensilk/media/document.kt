package org.opensilk.media

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.util.JsonReader
import android.util.JsonWriter

const val ROOTS_PARENT_ID = "\u2605G\u2605O\u2605D\u2605"

interface DocumentMeta: MediaMeta {
    val mimeType: String
    val lastMod: Long
    val flags: Long
}

interface DocumentRef: MediaRef {
    override val id: DocumentId
    override val meta: DocumentMeta
}

interface DocumentId: MediaId {
    val treeUri: Uri
    val documentId: String
    val parentId: String
}

/**
 * authority of the document provider
 */
val DocumentId.authority: String
    get() = treeUri.authority

/**
 * true if this is the top most uri we have access to
 */
val DocumentId.isRoot: Boolean
    get() = if (isTreeUri(treeUri)) {
        DocumentsContract.getTreeDocumentId(treeUri) == documentId
    } else {
        true
    }

/**
 * uri for playback
 */
val DocumentId.mediaUri: Uri
    get() = if (isTreeUri(treeUri)) {
        DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId)
    } else {
        DocumentsContract.buildDocumentUri(authority, documentId)
    }

/**
 * uri for loader
 */
val DocumentId.childrenUri: Uri
    get() = if (isTreeUri(treeUri)) {
        DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, documentId)
    } else {
        throw IllegalArgumentException("This documentId does not represent a tree")
    }

/**
 * documents can represent either a single document
 * or a single document made accessible through a tree,
 *
 * true if this was created using [Intent.ACTION_OPEN_DOCUMENT_TREE]
 */
val DocumentId.isFromTree: Boolean
    get() = isTreeUri(treeUri)

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
            DOCUMENT_DIRECTORY -> DocDirectoryId(
                    treeUri = Uri.parse(treeStr),
                    documentId = docStr,
                    parentId = parent)
            DOCUMENT_VIDEO -> DocVideoId(
                    treeUri = Uri.parse(treeStr),
                    documentId = docStr,
                    parentId = parent)
            DOCUMENT_MUSIC_TRACK -> DocMusicTrackId(
                    treeUri = Uri.parse(treeStr),
                    documentId = docStr,
                    parentId = parent)
            else -> TODO()
        }
    }

}