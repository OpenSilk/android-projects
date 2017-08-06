package org.opensilk.video

import android.media.browse.MediaBrowser
import io.reactivex.*
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import org.opensilk.common.misc.AlphanumComparator
import org.opensilk.media.*
import timber.log.Timber
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * The loader for the folder activity
 */
class UpnpFoldersLoader
@Inject constructor(
        private val mDatabaseClient: DatabaseClient,
        private val mBrowseLoader: UpnpBrowseLoader,
        private val mLookupService: LookupService
) {

    fun observable(mediaId: MediaId): Observable<List<MediaRef>> {
        val folderId = when (mediaId) {
            is UpnpFolderId -> mediaId
            is UpnpDeviceId -> UpnpFolderId(mediaId.deviceId, UPNP_ROOT_ID)
            else -> TODO("Unsupported mediaid")
        }
        val lookups = CompositeDisposable()
        //watch for system update id changes and re fetch list
        return mDatabaseClient.changesObservable
                //during lookup we can be flooded
                .filter { it is UpnpUpdateIdChange || (it is UpnpFolderChange && it.folderId == folderId) }
                .map { it is UpnpUpdateIdChange }
                .sample(5, TimeUnit.SECONDS)
                .startWith(true)
                .switchMapSingle { change ->
                    Timber.d("SwitchMap $folderId")
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
                }.doOnTerminate { lookups.clear() }
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

    /*
    private fun sendToLookup(metaList: List<MediaRef>): Disposable {
        return Observable.fromIterable(metaList)
                .filter { it is UpnpVideoRef }
                .flatMapCompletable { meta ->
                    mLookupService.lookupObservable(meta).firstOrError().flatMapCompletable({ lookup ->
                        associateMetaWithLookup(meta, lookup)
                    }).doOnError {
                        Timber.w("Error during lookup for ${meta.displayName} err=${it.message}")
                    }.onErrorComplete()
                }.subscribeOn(AppSchedulers.networkIo).subscribe()
    }

    private fun associateMetaWithLookup(meta: MediaMeta, lookup: MediaMeta): Completable {
        return Completable.fromAction {
            val videoId = newMediaRef(meta.mediaId).mediaId as UpnpVideoId
            val parentId = newMediaRef(meta.parentMediaId).mediaId as UpnpFolderId
            if (lookup.mimeType == MIME_TYPE_MOVIE) {
                Timber.d("Located movie association for ${meta.displayName}")
                mDatabaseClient.setUpnpVideoMovieId(videoId, lookup.rowId)
                if (meta.lookupName.isNotBlank()) {
                    mDatabaseClient.setMovieAssociation(meta.lookupName, meta.releaseYear, lookup.rowId)
                }
                mDatabaseClient.postChange(UpnpFolderChange(parentId))
                mDatabaseClient.postChange(UpnpVideoChange(videoId))
            } else if (lookup.mimeType == MIME_TYPE_TV_EPISODE) {
                Timber.d("Located episode association for ${meta.displayName}")
                mDatabaseClient.setUpnpVideoTvEpisodeId(videoId, lookup.rowId)
                if (meta.lookupName.isNotBlank()) {
                    mDatabaseClient.setTvSeriesAssociation(meta.lookupName, lookup.foreignRowId)
                }
                mDatabaseClient.postChange(UpnpFolderChange(parentId))
                mDatabaseClient.postChange(UpnpVideoChange(videoId))
            }
        }
    }
    */
}