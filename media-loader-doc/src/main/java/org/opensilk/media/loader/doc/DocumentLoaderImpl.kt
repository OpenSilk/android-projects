package org.opensilk.media.loader.doc

import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.provider.DocumentsContract
import io.reactivex.Maybe
import io.reactivex.Single
import org.opensilk.dagger2.ForApp
import org.opensilk.media.*
import timber.log.Timber
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

private fun Cursor.toDocumentId(parentId: DocumentId): DocumentId {
    return DocumentId(
            treeUri = parentId.treeUri,
            parentId = parentId.documentId,
            documentId = getString(0),
            mimeType = getString(2)
    )
}

private fun Cursor.toDirectoryDocumentRef(documentId: DocumentId): DirectoryDocumentRef {
    val displayName = getString(1)
    val mimeType = getString(2)
    //val size = if (!isNull(3)) getLong(3) else 0L
    val lastMod = if (!isNull(4)) getLong(4) else 0L
    val flags = getLong(5)
    return DirectoryDocumentRef(
            id = documentId,
            meta = DocumentMeta(
                    displayName = displayName,
                    mimeType = mimeType,
                    lastMod = lastMod,
                    flags = flags
            )
    )
}

private fun Cursor.toVideoDocumentRef(documentId: DocumentId): VideoDocumentRef {
    val displayName = getString(1)
    val mimeType = getString(2)
    val size = if (!isNull(3)) getLong(3) else 0L
    val lastMod = if (!isNull(4)) getLong(4) else 0L
    val flags = getLong(5)
    return VideoDocumentRef(
            id = documentId,
            meta = DocumentMeta(
                    displayName = displayName,
                    mimeType = mimeType,
                    size = size,
                    lastMod = lastMod,
                    flags = flags
            )
    )
}

/**
 * Created by drew on 8/9/17.
 */
class DocumentLoaderImpl @Inject constructor(@ForApp val mContext: Context): DocumentLoader {

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
                    val id = c.toDocumentId(documentId)
                    if (id.isDirectory) {
                        s.onSuccess(c.toDirectoryDocumentRef(id))
                    } else if (id.isVideo) {
                        s.onSuccess(c.toVideoDocumentRef(id))
                    } else {
                        TODO()
                    }
                } else {
                    s.onComplete()
                }
            } ?: s.onError(Exception("Unable to query document provider"))
        }
    }

    override fun directChildren(documentId: DocumentId, wantVideoItems: Boolean,
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
                    val id = c.toDocumentId(documentId)
                    if (id.isDirectory) {
                        list.add(c.toDirectoryDocumentRef(id))
                    } else if (wantVideoItems && id.isVideo) {
                        list.add(c.toVideoDocumentRef(id))
                    } else if (wantAudioItems && id.isAudio) {
                        TODO()
                    } //else ignore
                }
                s.onSuccess(list)
            } ?: s.onError(Exception("Unable to query document provider"))
        }
    }

}