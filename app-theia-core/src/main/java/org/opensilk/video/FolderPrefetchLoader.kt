package org.opensilk.video

import io.reactivex.disposables.Disposable
import io.reactivex.exceptions.Exceptions
import io.reactivex.functions.Consumer
import io.reactivex.subjects.PublishSubject
import org.opensilk.media.*
import org.opensilk.media.database.*
import org.opensilk.media.loader.cds.UpnpBrowseLoader
import org.opensilk.media.loader.doc.DocumentLoader
import org.opensilk.media.loader.storage.StorageLoader
import timber.log.Timber
import javax.inject.Inject

/**
 * Pre-fetches folders and inserts them into the database.
 *
 * The folders view model uses this to refresh the current item periodically
 * and to fetch its children for faster loads
 *
 * Created by drew on 8/26/17.
 */
class FolderPrefetchLoader @Inject constructor(
        private val mDatabaseClient: MediaDAO,
        private val mBrowseLoader: UpnpBrowseLoader,
        private val mDocumentLoader: DocumentLoader,
        private val mStorageLoader: StorageLoader
) {

    /**
     * Fetch children of [deviceId] and insert into database
     */
    fun prefetch(deviceId: MediaDeviceId, now: Boolean = false): Disposable = prefetch_(deviceId, now)

    /**
     * Fetch children of [folderId] and insert into database
     */
    fun prefetch(folderId: FolderId, now: Boolean = false): Disposable = prefetch_(folderId, now)

    private val mErrorSubject = PublishSubject.create<String>()

    fun errors(consumer: Consumer<String>): Disposable
            = mErrorSubject.hide().distinctUntilChanged().subscribe(consumer)

    private fun prefetch_(mediaId: MediaId, now: Boolean = false): Disposable = when (mediaId) {
        is UpnpDeviceId -> mBrowseLoader.directChildren(upnpFolderId = mediaId, wantVideoItems = true)
        is StorageDeviceId -> mStorageLoader.directChildren(parentId = mediaId, wantVideoItems = true)

        is UpnpFolderId -> mBrowseLoader.directChildren(upnpFolderId = mediaId, wantVideoItems = true)
        is DocDirectoryId -> mDocumentLoader.directChildren(documentId = mediaId, wantVideoItems = true)
        is StorageFolderId -> mStorageLoader.directChildren(parentId = mediaId, wantVideoItems = true)
        else -> TODO("$mediaId")
    }.subscribeOn(if (now) AppSchedulers.newThread else AppSchedulers.prefetch).subscribe({ itemList ->
        insertItems(mediaId, itemList)
        postChange(mediaId)
    }, { e ->
        Timber.e(e, "Unable to fetch items for $mediaId")
        mErrorSubject.onNext(e?.message ?: "Error loading items")
    })

    private fun insertItems(parentId: MediaId, itemList: List<MediaRef>) {
        Timber.d("Inserting ${itemList.size} children of $parentId")
        mDatabaseClient.hideChildrenOf(parentId)
        itemList.forEach { item ->
            when (item) {
                is UpnpFolderRef -> mDatabaseClient.addUpnpFolder(item)
                is UpnpVideoRef -> mDatabaseClient.addUpnpVideo(item)
                is StorageFolderRef -> mDatabaseClient.addStorageFolder(item)
                is StorageVideoRef -> mDatabaseClient.addStorageVideo(item)
                is DocDirectoryRef -> mDatabaseClient.addDocDirectory(item)
                is DocVideoRef -> mDatabaseClient.addDocVideo(item)
                else -> TODO("$item")
            }
        }
    }

    private fun postChange(parentId: MediaId) {
        mDatabaseClient.postChange(when (parentId) {
            is UpnpDeviceId -> UpnpDeviceChange(parentId)
            is StorageDeviceId -> StorageDeviceChange(parentId)

            is UpnpFolderId -> UpnpFolderChange(parentId)
            is DocDirectoryId -> DocDirectoryChange(parentId)
            is StorageFolderId -> StorageFolderChange(parentId)
            else -> TODO("$parentId")
        })
    }
}