package org.opensilk.video

import android.media.browse.MediaBrowser
import io.reactivex.Observable
import io.reactivex.ObservableSource
import io.reactivex.SingleSource
import org.opensilk.media.*
import timber.log.Timber
import javax.inject.Inject

/**
 * The loader for the folder activity
 */
class UpnpFoldersLoader
@Inject constructor(
        private val mDatabaseClient: DatabaseClient,
        private val mBrowseLoader: UpnpBrowseLoader
) {
    fun observable(mediaId: String): Observable<List<MediaBrowser.MediaItem>> {
        val mediaRef = newMediaRef(mediaId)
        val folderId = when (mediaRef.kind) {
            UPNP_FOLDER -> mediaRef.mediaId as UpnpFolderId
            UPNP_DEVICE -> UpnpFolderId((mediaRef.mediaId as UpnpDeviceId).deviceId, "0")
            else -> TODO("Unsupported mediaid")
        }
        //watch for system update id changes and re fetch list
        return Observable.just(true)
                .switchMapSingle { change ->
                    Timber.d("SwitchMap $folderId")
                    //first fetch from network and stick in database
                    //and then retrieve from the database to associate
                    // any metadata stored in database with network items
                    mBrowseLoader.completable(folderId).andThen(Observable.concat(
                            mDatabaseClient.getUpnpFolders(folderId),
                            mDatabaseClient.getUpnpVideos(folderId)
                        ).map { it.toMediaItem() }.toList()
                        //we get new thread since we have a rather complicated task
                    ).subscribeOn(AppSchedulers.newThread)
                }
    }
}