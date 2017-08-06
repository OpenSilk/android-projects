package org.opensilk.video

import android.media.browse.MediaBrowser
import io.reactivex.*
import io.reactivex.Observable
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

    fun observable(mediaRef: MediaRef): Observable<List<MediaBrowser.MediaItem>> {
        val folderId = when (mediaRef.kind) {
            UPNP_FOLDER -> mediaRef.mediaId as UpnpFolderId
            UPNP_DEVICE -> UpnpFolderId((mediaRef.mediaId as UpnpDeviceId).deviceId, UPNP_ROOT_ID)
            else -> TODO("Unsupported mediaid")
        }
        //watch for system update id changes and re fetch list
        return mDatabaseClient.changesObservable
                //during lookup we can be flooded
                .sample(5, TimeUnit.SECONDS)
                .filter { it is UpnpUpdateIdChange || (it is UpnpFolderChange && it.folderId == folderId) }
                .map { true }
                .startWith(true)
                .switchMapSingle { _ ->
                    Timber.d("SwitchMap $folderId")
                    //first fetch from network and stick in database
                    //and then retrieve from the database to associate
                    // any metadata stored in database with network items
                    // we get new thread because of rather complex operation
                    doNetwork(folderId).andThen(doDisk(folderId))
                            .doOnSuccess { sendToLookup(it) }
                            .subscribeOn(AppSchedulers.newThread)
                }
    }

    private fun doNetwork(folderId: UpnpFolderId): Completable {
        return mBrowseLoader.getDirectChildren(folderId)
                .flatMapCompletable { itemList ->
                    CompletableSource { s ->
                        mDatabaseClient.hideChildrenOf(folderId)
                        for (item in itemList) {
                            //insert/update remote item in database
                            val ref = newMediaRef(item.mediaId)
                            when (ref.kind) {
                                UPNP_FOLDER -> mDatabaseClient.addUpnpFolder(item)
                                UPNP_VIDEO -> mDatabaseClient.addUpnpVideo(item)
                                else -> Timber.e("Invalid kind slipped through %s for %s",
                                        ref.kind, item.displayName)
                            }
                        }
                        s.onComplete()
                    }
                }
    }

    private fun doDisk(folderId: UpnpFolderId): Single<List<MediaBrowser.MediaItem>> {
        return Observable.concat(
                mDatabaseClient.getUpnpFolders(folderId),
                mDatabaseClient.getUpnpVideos(folderId)
        ).map { it.toMediaItem() }.toList()
    }

    private fun sendToLookup(metaList: List<MediaBrowser.MediaItem>) {
        Observable.fromIterable(metaList).map { it._getMediaMeta() }
                .filter { newMediaRef(it.mediaId).kind == UPNP_VIDEO && !it.isParsed }
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
}