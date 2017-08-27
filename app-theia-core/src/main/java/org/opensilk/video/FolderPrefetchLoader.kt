package org.opensilk.video

import io.reactivex.Completable
import io.reactivex.disposables.Disposable
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
    fun prefetch(deviceId: MediaDeviceId, now: Boolean = false): Disposable = when (deviceId) {
        is UpnpDeviceId -> upnpCompletable(deviceId)
        is StorageDeviceId -> storageCompletable(deviceId)
        else -> TODO("$deviceId")
    }.subscribeOn(if (now) AppSchedulers.newThread else AppSchedulers.prefetch).subscribe({
        mDatabaseClient.postChange(when (deviceId) {
            is UpnpDeviceId -> UpnpDeviceChange()
            is StorageDeviceId -> StorageDeviceChange()
            else -> TODO("$deviceId")
        })
    })

    /**
     * Fetch children of [folderId] and insert into database
     */
    fun prefetch(folderId: FolderId, now: Boolean = false): Disposable = when (folderId) {
        is UpnpFolderId -> upnpCompletable(folderId)
        is DocDirectoryId -> documentCompletable(folderId)
        is StorageFolderId -> storageCompletable(folderId)
        else -> TODO("$folderId")
    }.subscribeOn(if (now) AppSchedulers.newThread else AppSchedulers.prefetch).subscribe({
        mDatabaseClient.postChange(when (folderId) {
            is UpnpFolderId -> UpnpFolderChange(folderId)
            is DocDirectoryId -> DocDirectoryChange(folderId)
            is StorageFolderId -> StorageFolderChange(folderId)
            else -> TODO("$folderId")
        })
    })

    private fun upnpCompletable(folderId: UpnpContainerId): Completable {
        return mBrowseLoader.directChildren(upnpFolderId = folderId, wantVideoItems = true)
                .flatMapCompletable { insertItemsCompletable(folderId, it) }
    }

    private fun storageCompletable(containerId: StorageContainerId): Completable {
        return mStorageLoader.directChildren(parentId = containerId, wantVideoItems = true)
                .flatMapCompletable { insertItemsCompletable(containerId, it) }
    }

    private fun documentCompletable(documentId: DocDirectoryId): Completable {
        return mDocumentLoader.directChildren(documentId = documentId, wantVideoItems = true)
                .flatMapCompletable({ insertItemsCompletable(documentId, it) })
    }

    private fun insertItemsCompletable(parentId: MediaId, itemList: List<MediaRef>): Completable {
        return Completable.fromAction {
            mDatabaseClient.hideChildrenOf(parentId)
            itemList.forEach { item ->
                Timber.v("Inserting $item")
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
    }
}