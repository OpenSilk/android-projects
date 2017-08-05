package org.opensilk.video

import android.media.browse.MediaBrowser
import io.reactivex.Observable
import io.reactivex.ObservableSource
import io.reactivex.SingleSource
import org.opensilk.media.*
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
        return mDatabaseClient.changesObservable
                .filter { it is UpnpUpdateIdChange }
                .startWith { UpnpUpdateIdChange() }
                .switchMapSingle {
                    //first fetch from network and stick in database
                    mBrowseLoader.completable(folderId).andThen<List<MediaBrowser.MediaItem>>(SingleSource {
                        //and then retrieve the items back from the database
                        Observable.concat(
                                mDatabaseClient.getUpnpFolders(folderId),
                                mDatabaseClient.getUpnpVideos(folderId)
                        ).map {
                            it.toMediaItem()
                        }.toList()
                        //we get new thread since we have a rather complicated task
                    }).subscribeOn(AppSchedulers.newThread)
                }
    }
}