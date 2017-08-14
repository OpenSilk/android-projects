package org.opensilk.video

import io.reactivex.Completable
import io.reactivex.CompletableSource
import io.reactivex.Observable
import io.reactivex.Single
import org.opensilk.media.*
import org.opensilk.media.database.*
import org.opensilk.media.loader.cds.UpnpBrowseLoader
import org.opensilk.media.loader.doc.DocumentLoader
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * The loader for the folder activity
 */
class FoldersLoader
@Inject constructor(
        private val mDatabaseClient: MediaDAO,
        private val mBrowseLoader: UpnpBrowseLoader,
        private val mDocumentLoader: DocumentLoader
) {

    fun observable(mediaId: MediaId): Observable<out List<MediaRef>> {
        return when (mediaId) {
            is UpnpFolderId -> upnpFolderIdObservable(mediaId)
            is UpnpDeviceId -> upnpFolderIdObservable(mediaId)
            is DocumentId -> documentIdLoader(mediaId)
            else -> TODO("Unsupported mediaid")
        }
    }

    private fun upnpFolderIdObservable(folderId: UpnpContainerId): Observable<List<MediaRef>> {
        val videoIds = HashSet<UpnpVideoId>()
        //watch for system update id changes and re fetch list
        return mDatabaseClient.changesObservable
                //during lookup we can be flooded
                .filter { it is UpnpUpdateIdChange || (it is UpnpVideoChange && videoIds.contains(it.videoId)) }
                .map { it is UpnpUpdateIdChange }
                .sample(5, TimeUnit.SECONDS)
                .startWith(true)
                .switchMapSingle { change ->
                    //first fetch from network and stick in database
                    //and then retrieve from the database to associate
                    // any metadata stored in database with network items
                    // we get new thread because of rather complex operation
                    if (change) {
                        videoIds.clear()
                        doNetwork(folderId).andThen(doDisk(folderId, videoIds))
                                .subscribeOn(AppSchedulers.newThread)
                    } else {
                        doDisk(folderId, videoIds).subscribeOn(AppSchedulers.diskIo)
                    }
                }
    }

    private fun doNetwork(folderId: UpnpContainerId): Completable {
        return mBrowseLoader.directChildren(upnpFolderId = folderId, wantVideoItems = true)
                .flatMapCompletable { itemList ->
                    CompletableSource { s ->
                        mDatabaseClient.hideChildrenOf(folderId)
                        itemList.forEach { item ->
                            //insert/update remote item in database
                            when (item) {
                                is UpnpFolderRef -> mDatabaseClient.addUpnpFolder(item)
                                is UpnpVideoRef -> mDatabaseClient.addUpnpVideo(item)
                                else -> Timber.e("Invalid kind slipped through ${item::class}")
                            }
                        }
                        s.onComplete()
                    }
                }
    }

    private fun doDisk(folderId: UpnpContainerId, videoIds: MutableSet<UpnpVideoId>): Single<out List<MediaRef>> {
        return Observable.concat(
                mDatabaseClient.getUpnpFoldersUnder(folderId),
                mDatabaseClient.getUpnpVideosUnder(folderId).doOnNext { videoIds.add(it.id) }
        ).toList()
    }

    private fun documentIdLoader(documentId: DocumentId): Observable<List<DocumentRef>> {
        //watch for system update id changes and re fetch list
        return mDatabaseClient.changesObservable
                //during lookup we can be flooded
                .filter { it is VideoDocumentChange && it.documentId.treeUri == documentId.treeUri }
                .map { true }
                .sample(5, TimeUnit.SECONDS)
                .startWith(true)
                .switchMapSingle {
                    getDocuments(documentId).andThen(getDocumentsLocal(documentId))
                            .subscribeOn(AppSchedulers.newThread)
                }
    }

    private fun getDocuments(documentId: DocumentId): Completable {
        return mDocumentLoader.directChildren(documentId = documentId, wantVideoItems = true)
                .flatMapCompletable { list ->
                    Completable.fromAction {
                        mDatabaseClient.hideChildrenOf(documentId)
                        list.forEach { doc ->
                            when (doc) {
                                is DirectoryDocumentRef -> mDatabaseClient.addDirectoryDocument(doc)
                                is VideoDocumentRef -> mDatabaseClient.addVideoDocument(doc)
                                else -> Timber.e("Invalid kind slipped through ${doc::class}")
                            }
                        }
                    }
                }
    }

    private fun getDocumentsLocal(documentId: DocumentId): Single<List<DocumentRef>> {
        return Observable.concat<DocumentRef>(
                mDatabaseClient.getDirectoryDocumentsUnder(documentId),
                mDatabaseClient.getVideoDocumentsUnder(documentId)
        ).toList()
    }

}