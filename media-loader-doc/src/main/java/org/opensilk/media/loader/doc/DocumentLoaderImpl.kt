package org.opensilk.media.loader.doc

import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.provider.DocumentsContract
import io.reactivex.Maybe
import io.reactivex.Single
import org.opensilk.dagger2.ForApp
import org.opensilk.media.*
import javax.inject.Inject
import kotlin.collections.ArrayList

private val DOCUMENT_COLS = arrayOf(
        DocumentsContract.Document.COLUMN_DOCUMENT_ID,
        DocumentsContract.Document.COLUMN_DISPLAY_NAME,
        DocumentsContract.Document.COLUMN_MIME_TYPE,
        DocumentsContract.Document.COLUMN_SIZE,
        DocumentsContract.Document.COLUMN_LAST_MODIFIED,
        DocumentsContract.Document.COLUMN_FLAGS
)

private fun Cursor.toDirectoryDocumentRef(parentId: DocumentId): DocDirectoryRef {
    val id = getString(0)
    val displayName = getString(1)
    val mimeType = getString(2)
    //val size = if (!isNull(3)) getLong(3) else 0L
    val lastMod = if (!isNull(4)) getLong(4) else 0L
    val flags = getLong(5)
    return DocDirectoryRef(
            id = DocDirectoryId(
                    treeUri = parentId.treeUri,
                    parentId = parentId.documentId,
                    documentId = id
            ),
            meta = DocDirectoryMeta(
                    title = displayName,
                    mimeType = mimeType,
                    lastMod = lastMod,
                    flags = flags
            )
    )
}

private fun Cursor.toVideoDocumentRef(parentId: DocumentId): DocVideoRef {
    val id = getString(0)
    val displayName = getString(1)
    val mimeType = getString(2)
    val size = if (!isNull(3)) getLong(3) else 0L
    val lastMod = if (!isNull(4)) getLong(4) else 0L
    val flags = getLong(5)
    val vidId = DocVideoId(
            treeUri = parentId.treeUri,
            parentId = parentId.documentId,
            documentId = id
    )
    return DocVideoRef(
            id = vidId,
            meta = DocVideoMeta(
                    title = displayName,
                    mimeType = mimeType,
                    size = size,
                    lastMod = lastMod,
                    flags = flags,
                    mediaUri = vidId.mediaUri
            )
    )
}

/**
 * Created by drew on 8/9/17.
 */
class DocumentLoaderImpl @Inject constructor(
        @ForApp private val mContext: Context
): DocumentLoader {

    override fun document(documentId: DocumentId): Maybe<DocumentRef> {
        val uriPermission = mContext.contentResolver.persistedUriPermissions
                .firstOrNull { it.uri == documentId.treeUri }
                ?: return Maybe.error(Exception("Not permitted to access uri. Please reselect item or folder"))
        //touch refresh time
        mContext.contentResolver.takePersistableUriPermission(uriPermission.uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        return Maybe.create { s ->
            mContext.contentResolver.query(documentId.mediaUri, DOCUMENT_COLS,
                    null, null, DocumentsContract.Document.COLUMN_DISPLAY_NAME)?.use { c ->
                if (c.moveToFirst()) {
                    val mime = c.getString(2)
                    val doc = when {
                        mime == DocumentsContract.Document.MIME_TYPE_DIR -> c.toDirectoryDocumentRef(documentId)
                        mime.startsWith("video") -> c.toVideoDocumentRef(documentId)
                        else -> TODO()
                    }
                    s.onSuccess(doc)
                } else {
                    s.onComplete()
                }
            } ?: s.onError(Exception("Unable to query document provider"))
        }
    }

    override fun directChildren(documentId: DocDirectoryId, wantVideoItems: Boolean,
                                wantAudioItems: Boolean): Single<out List<DocumentRef>> {
        if (!documentId.isFromTree) {
            return Single.error(Exception("Document does not represent a tree"))
        }
        val uriPermission = mContext.contentResolver.persistedUriPermissions
                .firstOrNull { it.uri == documentId.treeUri }
                ?: return Single.error(Exception("Not permitted to access uri. Please reselect item or folder"))
        //touch refresh time
        mContext.contentResolver.takePersistableUriPermission(uriPermission.uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        return Single.create { s ->
            mContext.contentResolver.query(documentId.childrenUri, DOCUMENT_COLS,
                    null, null, DocumentsContract.Document.COLUMN_DISPLAY_NAME)?.use { c ->
                val list = ArrayList<DocumentRef>()
                while (c.moveToNext()) {
                    val mime = c.getString(2)
                    val doc = when {
                        mime == DocumentsContract.Document.MIME_TYPE_DIR -> c.toDirectoryDocumentRef(documentId)
                        mime.startsWith("video") && wantVideoItems -> c.toVideoDocumentRef(documentId)
                        else -> null
                    } ?: continue
                    list.add(doc)
                }
                s.onSuccess(list)
            } ?: s.onError(Exception("Unable to query document provider"))
        }
    }

}