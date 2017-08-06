package org.opensilk.video

import android.media.browse.MediaBrowser
import io.reactivex.*
import org.opensilk.media.*
import timber.log.Timber
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
    fun observable(mediaId: String): Observable<List<MediaBrowser.MediaItem>> {
        val mediaRef = newMediaRef(mediaId)
        val folderId = when (mediaRef.kind) {
            UPNP_FOLDER -> mediaRef.mediaId as UpnpFolderId
            UPNP_DEVICE -> UpnpFolderId((mediaRef.mediaId as UpnpDeviceId).deviceId, UPNP_ROOT_ID)
            else -> TODO("Unsupported mediaid")
        }
        //watch for system update id changes and re fetch list
        return mDatabaseClient.changesObservable
                .filter { it is UpnpUpdateIdChange || (it is UpnpFolderChange && it.folderId == folderId) }
                .map { true }
                .startWith(true)
                .switchMapSingle { _ ->
                    Timber.d("SwitchMap $folderId")
                    //first fetch from network and stick in database
                    //and then retrieve from the database to associate
                    // any metadata stored in database with network items
                    // we get new thread because of rather complex operation
                    doNetwork(folderId).andThen(doDisk(folderId)).subscribeOn(AppSchedulers.newThread)
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
                mDatabaseClient.getUpnpVideos(folderId)).map { it.toMediaItem() }.toList()
    }

}