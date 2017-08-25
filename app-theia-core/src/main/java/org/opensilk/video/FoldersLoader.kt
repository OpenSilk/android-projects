package org.opensilk.video

import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import org.opensilk.media.*
import org.opensilk.media.database.*
import org.opensilk.media.loader.cds.UpnpBrowseLoader
import org.opensilk.media.loader.doc.DocumentLoader
import org.opensilk.media.loader.storage.StorageLoader
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * The loader for the folder activity
 */
class FoldersLoader
@Inject constructor(
        private val mDatabaseClient: MediaDAO,
        private val mBrowseLoader: UpnpBrowseLoader,
        private val mDocumentLoader: DocumentLoader,
        private val mStorageLoader: StorageLoader
) {

    fun observable(mediaId: MediaId): Observable<out List<MediaRef>> = when (mediaId) {
        is UpnpContainerId -> upnpObservable(mediaId)
        is StorageContainerId -> storageObservable(mediaId)
        is DocumentId -> documentObservable(mediaId)
        else -> TODO("Unsupported mediaid")
    }

    private fun upnpObservable(folderId: UpnpContainerId): Observable<List<MediaRef>> {
        //watch for system update id changes and re fetch list
        return mDatabaseClient.changesObservable
                .filter {
                    when (it) {
                        is UpnpUpdateIdChange -> true
                        is UpnpVideoChange -> it.videoId.parentId == folderId.containerId
                        else -> false
                    }
                }
                .map { it is UpnpUpdateIdChange }
                //during lookup we can be flooded
                .sample(3, TimeUnit.SECONDS)
                .startWith(true)
                .switchMapSingle { change ->
                    //first fetch from network and stick in database
                    //and then retrieve from the database to associate
                    // any metadata stored in database with network items
                    // we get new thread because of rather complex operation
                    if (change) {
                        upnpCompletable(folderId).andThen(upnpDisk(folderId))
                                .subscribeOn(AppSchedulers.newThread)
                    } else {
                        upnpDisk(folderId)
                                .subscribeOn(AppSchedulers.diskIo)
                    }
                }
    }

    private fun upnpCompletable(folderId: UpnpContainerId): Completable {
        return mBrowseLoader.directChildren(upnpFolderId = folderId, wantVideoItems = true)
                .flatMapCompletable { insertItemsCompletable(folderId, it) }
    }

    private fun upnpDisk(folderId: UpnpContainerId): Single<List<UpnpRef>> {
        return Observable.concat(
                mDatabaseClient.getUpnpFoldersUnder(folderId),
                mDatabaseClient.getUpnpVideosUnder(folderId)
        ).toList()
    }

    private fun storageObservable(mediaId: StorageContainerId): Observable<List<StorageRef>> {
        return mDatabaseClient.changesObservable
                .filter { it is StorageVideoChange && it.videoId.parent == mediaId.path }
                .map { true }
                .sample(3, TimeUnit.SECONDS)
                .startWith(true)
                .switchMapSingle {
                    storageCompletable(mediaId).andThen(storageDisk(mediaId))
                            .subscribeOn(AppSchedulers.newThread)
                }
    }

    private fun storageCompletable(containerId: StorageContainerId): Completable {
        return mStorageLoader.directChildren(parentId = containerId, wantVideoItems = true)
                .flatMapCompletable { insertItemsCompletable(containerId, it) }
    }

    private fun storageDisk(containerId: StorageContainerId): Single<List<StorageRef>> {
        return Observable.concat(
                mDatabaseClient.getStorageFoldersUnder(containerId),
                mDatabaseClient.getStorageVideosUnder(containerId)
        ).toList()
    }

    private fun documentObservable(documentId: DocumentId): Observable<List<DocumentRef>> {
        //watch for system update id changes and re fetch list
        return mDatabaseClient.changesObservable
                //during lookup we can be flooded
                .filter { it is VideoDocumentChange && it.documentId.parentId == documentId.documentId }
                .map { true }
                .sample(3, TimeUnit.SECONDS)
                .startWith(true)
                .switchMapSingle {
                    documentCompletable(documentId).andThen(documentDisk(documentId))
                            .subscribeOn(AppSchedulers.newThread)
                }
    }

    private fun documentCompletable(documentId: DocumentId): Completable {
        return mDocumentLoader.directChildren(documentId = documentId, wantVideoItems = true)
                .flatMapCompletable({ insertItemsCompletable(documentId, it) })
    }

    private fun documentDisk(documentId: DocumentId): Single<List<DocumentRef>> {
        return Observable.concat<DocumentRef>(
                mDatabaseClient.getDirectoryDocumentsUnder(documentId),
                mDatabaseClient.getVideoDocumentsUnder(documentId)
        ).toList()
    }

    private fun insertItemsCompletable(parentId: MediaId, itemList: List<MediaRef>): Completable {
        return Completable.fromAction {
            mDatabaseClient.hideChildrenOf(parentId)
            itemList.forEach { item ->
                when (item) {
                    is UpnpFolderRef -> mDatabaseClient.addUpnpFolder(item)
                    is UpnpVideoRef -> mDatabaseClient.addUpnpVideo(item)
                    is StorageFolderRef -> mDatabaseClient.addStorageFolder(item)
                    is StorageVideoRef -> mDatabaseClient.addStorageVideo(item)
                    is DocDirectoryRef -> mDatabaseClient.addDirectoryDocument(item)
                    is DocVideoRef -> mDatabaseClient.addVideoDocument(item)
                    else -> TODO("${item::javaClass::name}")
                }
            }
        }
    }

}