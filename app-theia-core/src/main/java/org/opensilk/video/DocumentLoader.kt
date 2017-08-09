package org.opensilk.video

import android.content.Context
import android.database.Cursor
import android.media.browse.MediaBrowser
import android.provider.DocumentsContract
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

    fun documents(documentId: DocumentId): Single<List<DocumentRef>> {
        return Single.create<List<DocumentRef>> { s ->
            mContext.contentResolver.query(documentId.childrenUri, DOCUMENT_COLS,
                    null, null, DocumentsContract.Document.COLUMN_DISPLAY_NAME)?.use { c ->
                val list = ArrayList<DocumentRef>()
                while (c.moveToNext()) {
                    list.add(c.toDocumentRef(documentId))
                }
                s.onSuccess(list)
            } ?: s.onError(Exception("Unable to query document provider"))
        }
    }

}