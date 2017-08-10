package org.opensilk.music

import android.content.ContentValues
import android.content.Context
import android.media.browse.MediaBrowser
import android.net.Uri
import android.provider.DocumentsContract
import org.opensilk.common.dagger.ForApplication
import org.opensilk.media.DocumentRef
import org.opensilk.media.MediaRef
import timber.log.Timber
import java.util.*
import javax.inject.Inject

/**
 *
 */
class MusicDbClient
@Inject
constructor(
        @ForApplication private val mContext: Context,
        private val mUris: MusicDbUris
) {

    fun removeOrphanDocs(parentRef: DocumentRef, currentChildren: List<DocumentRef>) {
        mContext.contentResolver.query(mUris.mediaDocs(),
                arrayOf("tree_uri", "document_id"),
                "parent_document_id = ? AND tree_uri = ?",
                arrayOf(parentRef.documentId, parentRef.treeUri.toString()), null).use { c ->
            if (c.moveToFirst()) {
                do {
                    val childRef = DocumentRef(Uri.parse(c.getString(0)), c.getString(1))
                    if (childRef !in currentChildren) {
                        removeDoc(childRef)
                    }
                } while (c.moveToNext())
            }
        }
    }

    private fun removeDoc(docRef: DocumentRef): Int {
        val isDir = mContext.contentResolver.query(mUris.mediaDocs(), arrayOf("mime_type"),
                MEDIA_DOCUMENT_SEL, mediaDocSelArgs(docRef), null).use { c ->
            if (c.moveToFirst()) {
                val mime = c.getString(0)
                return@use mime == DocumentsContract.Document.MIME_TYPE_DIR
            } else {
                return@use false
            }
        }
        var num = 0
        if (isDir) {
            val children: List<DocumentRef> = mContext.contentResolver.query(
                    mUris.mediaDocs(), arrayOf("tree_uri", "document_id"),
                    MEDIA_DOCUMENT_PARENT_SEL, mediaDocSelArgs(docRef), null).use { c ->
                return@use if (c.moveToFirst()) {
                    ArrayList<DocumentRef>(c.count).apply {
                        do {
                            add(DocumentRef(Uri.parse(c.getString(0)), c.getString(1)))
                        } while (c.moveToNext())
                    }
                } else {
                    emptyList<DocumentRef>()
                }
            }
            children.forEach {
                num += removeDoc(it)
            }
        }
        num += mContext.contentResolver.delete(mUris.mediaDocs(),
                MEDIA_DOCUMENT_SEL, mediaDocSelArgs(docRef))
        return num
    }

    fun insertRootDoc(rootRef: DocumentRef): Boolean {
        return mContext.contentResolver.query(rootRef.mediaUri,
                DOCUMENT_COLS, null, null, null)?.use { c ->
            if (c.moveToFirst()) {
                val colMap = c.mapCols(DOCUMENT_COLS)
                val parentRootRef = rootRef.copy(documentId = ROOTS_PARENT)
                val rootMedia = c.makeDocMediaItem(colMap, parentRootRef)
                val rootMeta = rootMedia._getMediaMeta()
                val returnedRef = rootMedia._getMediaRef()
                //make sure they sent us the document we requested
                val isSameDocument = returnedRef.mediaId == rootRef.mediaId
                if (isSameDocument && rootMeta.isDirectory) {
                    return@use insertMediaDoc(rootMedia)
                } else {
                    return@use false
                }
            } else {
                return@use false
            }
        } ?: false
    }

    fun removeRootDoc(rootRef: DocumentRef): Boolean {
        return removeDoc(rootRef) > 0
    }

    fun getRootDocs(): List<MediaBrowser.MediaItem> {
        return mContext.contentResolver.query(mUris.mediaDocs(), ROOT_COLS,
                "parent_document_id = ?", arrayOf(ROOTS_PARENT),
                DocumentsContract.Document.COLUMN_DISPLAY_NAME).use { c ->
            if (c.moveToFirst()) {
                val colMap = c.mapCols(ROOT_COLS)
                val lst = ArrayList<MediaBrowser.MediaItem>(c.count)
                do {
                    lst.add(c.makeDocMediaItem(colMap))
                } while (c.moveToNext())
                return@use lst
            } else {
                return@use emptyList()
            }
        }
    }

    fun insertMediaDoc(mediaItem: MediaBrowser.MediaItem): Boolean {
        val mediaMeta = mediaItem._getMediaMeta()
        val cv = ContentValues()

        val docRef = mediaItem._getMediaRef()
        if (docRef !is DocumentRef) {
            Timber.w("Media %s is not a document", mediaMeta.displayName)
            return false
        }

        val parentRef = MediaRef.parse(mediaMeta.parentMediaId)
        if (parentRef !is DocumentRef) {
            Timber.w("Media %s does not have a valid parent id=%s",
                    mediaMeta.displayName, mediaMeta.parentMediaId)
            return false
        }

        cv.put(DocumentsContract.Document.COLUMN_DISPLAY_NAME, mediaMeta.displayName)
        cv.put(DocumentsContract.Document.COLUMN_MIME_TYPE, mediaMeta.mimeType)
        cv.put(DocumentsContract.Document.COLUMN_SIZE, mediaMeta.size)
        cv.put(DocumentsContract.Document.COLUMN_LAST_MODIFIED, mediaMeta.lastModified)
        cv.put(DocumentsContract.Document.COLUMN_FLAGS, mediaMeta.documentFlags)

        cv.put("authority", docRef.authority)
        cv.put("parent_document_id", parentRef.documentId)
        cv.put("title", mediaItem.description.title?.toString() ?: mediaMeta.displayName)
        cv.put("subtitle", mediaItem.description.subtitle?.toString())

        if (mediaMeta.isAudio) {
            cv.put("album_name", mediaMeta.albumName)
            cv.put("artist_name", mediaMeta.artistName)
            cv.put("album_artist_name", mediaMeta.genreName)
            cv.put("genre_name", mediaMeta.genreName)
            cv.put("release_year", mediaMeta.releaseYear)
            cv.put("bitrate", mediaMeta.bitrate)
            cv.put("duration", mediaMeta.duration)
            cv.put("compilation", if (mediaMeta.isCompilation) 1 else 0)
            cv.put("track_number", mediaMeta.trackNumber)
            cv.put("disc_number", mediaMeta.discNumber)
            if (mediaMeta.artworkUri != Uri.EMPTY) {
                cv.put("artwork_uri", mediaMeta.artworkUri.toString())
            }
            if (mediaMeta.backdropUri != Uri.EMPTY) {
                cv.put("backdrop_uri", mediaMeta.backdropUri.toString())
            }
            cv.put("headers", mediaMeta.mediaHeaders)
        }

        val num = mContext.contentResolver.update(mUris.mediaDocs(), cv,
                MEDIA_DOCUMENT_SEL, mediaDocSelArgs(docRef))
        if (num > 0) {
            Timber.d("Updated media %s", mediaMeta.displayName)
            return true
        }
        cv.put(DocumentsContract.Document.COLUMN_DOCUMENT_ID, docRef.documentId)
        cv.put("tree_uri", docRef.treeUri.toString())
        cv.put("date_added", System.currentTimeMillis())
        return mContext.contentResolver.insert(mUris.mediaDocs(), cv) != null
    }

    fun getMediaDoc(docRef: DocumentRef): rx.Single<MediaBrowser.MediaItem> {
        return rx.Single.create { s ->
            val item = mContext.contentResolver.query(mUris.mediaDocs(), MEDIA_COLS,
                    MEDIA_DOCUMENT_SEL, mediaDocSelArgs(docRef), null).use { c ->
                if (c.moveToFirst()) {
                    val colMap = c.mapCols(MEDIA_COLS)
                    return@use c.makeDocMediaItem(colMap)
                } else {
                    return@use null
                }
            }
            if (item == null) {
                s.onError(NoSuchDocumentException())
            } else {
                s.onSuccess(item)
            }
        }
    }

    fun getDocChildren(docRef: DocumentRef) : List<MediaBrowser.MediaItem> {
        return mContext.contentResolver.query(mUris.mediaDocs(), MEDIA_COLS,
                "parent_document_id = ? AND tree_uri = ?",
                arrayOf(docRef.documentId, docRef.treeUri.toString()),
                DocumentsContract.Document.COLUMN_DISPLAY_NAME).use { c ->
            if (c.moveToFirst()) {
                val colMap = c.mapCols(MEDIA_COLS)
                val lst = ArrayList<MediaBrowser.MediaItem>(c.count)
                do {
                    lst.add(c.makeDocMediaItem(colMap))
                } while (c.moveToNext())
                return@use lst
            } else {
                return@use emptyList()
            }
        }
    }

    /**
     * Fetches the mediaItem for the passed docRef. If it is a playable item just the item
     * is returned. If it is a browsable item, all playable children are returned
     */
    fun getMediaRecursive(docRef: DocumentRef): rx.Observable<MediaBrowser.MediaItem> {
        return rx.Observable.create<MediaBrowser.MediaItem> { s ->
            mContext.contentResolver.query(docRef.mediaUri, DOCUMENT_COLS, null, null, null)?.use { c ->
                if (!c.moveToFirst()) {
                    s.onError(NoSuchDocumentException())
                    return@use
                }
                val colMap = c.mapCols(DOCUMENT_COLS)
                val docMedia = c.makeDocMediaItem(colMap, docRef)
                //make sure they sent us the document we requested
                if (docMedia._getMediaRef().mediaId != docRef.mediaId) {
                    s.onError(NoSuchDocumentException())
                    return@use
                }
                s.onNext(docMedia)
                s.onCompleted()
            } ?: s.onError(NoSuchDocumentException())
        }.concatMap<MediaBrowser.MediaItem> { item ->
            return@concatMap if (item.isBrowsable) {
                getChildrenRecursive(item)
            } else if (item.isPlayable) {
                Observable.just(item)
            } else {
                Observable.error(InvalidMediaException())
            }
        }.filter { item -> item.isPlayable }
    }

    fun getChildrenRecursive(item: MediaBrowser.MediaItem): Observable<MediaBrowser.MediaItem> {
        return Observable.create<MediaBrowser.MediaItem> { s ->
            val ref = item._getDocRef()
            mContext.contentResolver.query(ref.childrenUri, DOCUMENT_COLS,
                    null, null, DocumentsContract.Document.COLUMN_DISPLAY_NAME)?.use { c ->
                if (!c.moveToFirst()) {
                    s.onError(NoSuchDocumentException())
                    return@use
                }
                val colMap = c.mapCols(DOCUMENT_COLS)
                do {
                    s.onNext(c.makeDocMediaItem(colMap, ref))
                } while (c.moveToNext())
                s.onCompleted()
            } ?: s.onError(NoSuchDocumentException())
        }.concatMap<MediaBrowser.MediaItem> { item ->
            return@concatMap if (item.isBrowsable) {
                getChildrenRecursive(item) //recursive call
            } else {
                Observable.just(item)
            }
        }
    }
}