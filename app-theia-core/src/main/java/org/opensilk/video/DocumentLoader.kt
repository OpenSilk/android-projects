package org.opensilk.video

import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.media.browse.MediaBrowser
import android.net.Uri
import android.provider.DocumentsContract
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.Single
import org.opensilk.common.dagger.ForApplication
import org.opensilk.media.DocumentId
import org.opensilk.media.DocumentMeta
import org.opensilk.media.DocumentRef
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

private fun Cursor.toDocumentRef(parentId: DocumentId): DocumentRef {
    val docId = getString(0)
    val displayName = getString(1)
    val mimeType = getString(2)
    val size = if (!isNull(3)) getLong(3) else 0L
    val lastMod = if (!isNull(4)) getLong(4) else 0L
    val flags = getLong(5)
    return DocumentRef(
            id = DocumentId(
                    treeUri = parentId.treeUri,
                    parentId = parentId.documentId,
                    documentId = docId
            ),
            meta = DocumentMeta(
                    displayName = displayName,
                    summary = "",
                    mimeType = mimeType,
                    size =  size,
                    lastMod = lastMod,
                    flags = flags
            )
    )
}

/**
 * Created by drew on 8/9/17.
 */
class DocumentLoader
@Inject constructor(
        @ForApplication val mContext: Context
) {

    fun document(documentId: DocumentId): Maybe<DocumentRef> {
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
                    s.onSuccess(c.toDocumentRef(documentId))
                } else {
                    s.onComplete()
                }
            } ?: s.onError(Exception("Unable to query document provider"))
        }
    }

    fun documents(documentId: DocumentId): Maybe<List<DocumentRef>> {
        if (!documentId.isFromTree) {
            return Maybe.error(Exception("Document does not represent a tree"))
        }
        val uriPermission = mContext.contentResolver.persistedUriPermissions
                .firstOrNull { it.uri == documentId.treeUri }
                ?: return Maybe.error(Exception("Not permitted to access uri. Please reselect item or folder"))
        //touch refresh time
        mContext.contentResolver.takePersistableUriPermission(uriPermission.uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        return Maybe.create { s ->
            mContext.contentResolver.query(documentId.childrenUri, DOCUMENT_COLS,
                    null, null, DocumentsContract.Document.COLUMN_DISPLAY_NAME)?.use { c ->
                val list = ArrayList<DocumentRef>()
                while (c.moveToNext()) {
                    list.add(c.toDocumentRef(documentId))
                }
                if (list.isNotEmpty()) {
                    s.onSuccess(list)
                } else {
                    s.onComplete()
                }
            } ?: s.onError(Exception("Unable to query document provider"))
        }
    }

}