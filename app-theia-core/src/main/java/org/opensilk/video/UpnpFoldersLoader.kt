package org.opensilk.video

import io.reactivex.Completable
import io.reactivex.CompletableSource
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import org.opensilk.media.*
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * The loader for the folder activity
 */
class UpnpFoldersLoader
@Inject constructor(
        private val mDatabaseClient: DatabaseClient,
        private val mBrowseLoader: UpnpBrowseLoader
) {

    fun observable(mediaId: MediaId): Observable<List<MediaRef>> {
        val folderId = when (mediaId) {
            is UpnpFolderId -> mediaId
            is UpnpDeviceId -> UpnpFolderId(mediaId.deviceId, UPNP_ROOT_ID)
            else -> TODO("Unsupported mediaid")
        }
        //watch for system update id changes and re fetch list
        return mDatabaseClient.changesObservable
                //during lookup we can be flooded
                .filter { it is UpnpUpdateIdChange || (it is UpnpFolderChange && it.folderId == folderId) }
                .map { it is UpnpUpdateIdChange }
                .sample(5, TimeUnit.SECONDS)
                .startWith(true)
                .switchMapSingle { change ->
                    //first fetch from network and stick in database
                    //and then retrieve from the database to associate
                    // any metadata stored in database with network items
                    // we get new thread because of rather complex operation
                    if (change) {
                        doNetwork(folderId).andThen(doDisk(folderId))
                                //.doOnSuccess { lookups.add(sendToLookup(it)) }
                                .subscribeOn(AppSchedulers.newThread)
                    } else {
                        doDisk(folderId).subscribeOn(AppSchedulers.diskIo)
                    }
                }
    }

    private fun doNetwork(folderId: UpnpFolderId): Completable {
        return mBrowseLoader.getDirectChildren(folderId)
                .flatMapCompletable { itemList ->
                    CompletableSource { s ->
                        mDatabaseClient.hideChildrenOf(folderId)
                        for (item in itemList) {
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

    private fun doDisk(folderId: UpnpFolderId): Single<List<MediaRef>> {
        return Observable.concat(
                mDatabaseClient.getUpnpFoldersUnder(folderId),
                mDatabaseClient.getUpnpVideosUnder(folderId)
        ).toList()
    }

}