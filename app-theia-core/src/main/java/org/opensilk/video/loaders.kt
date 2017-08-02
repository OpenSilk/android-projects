package org.opensilk.video

import android.media.browse.MediaBrowser
import io.reactivex.Observable
import org.opensilk.media.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * The Loader for the Media Servers row in the Home Activity
 */
class UpnpDevicesLoader
@Inject constructor(
        private val mDatabaseClient: DatabaseClient
){
    val observable: Observable<List<MediaBrowser.MediaItem>> by lazy {
        mDatabaseClient.changesObservable
                .filter { it is UpnpDeviceChange }
                .map { it as UpnpDeviceChange }
                .startWith(UpnpDeviceChange())
                .observeOn(AppSchedulers.diskIo)
                .flatMapSingle {
                    mDatabaseClient.getUpnpDevices().map { it.toMediaItem() }.toList()
                }
    }
}

/**
 * The loader for the folder activity
 */
class UpnpFoldersLoader
@Inject constructor(
        private val mDatabaseClient: DatabaseClient
) {
    fun observable(mediaId: String): Observable<List<MediaBrowser.MediaItem>> {
        val mediaRef = newMediaRef(mediaId)
        val folderId = when (mediaRef.kind) {
            UPNP_FOLDER -> mediaRef.mediaId as UpnpFolderId
            UPNP_DEVICE -> UpnpFolderId((mediaRef.mediaId as UpnpDeviceId).deviceId, "0")
            else -> TODO("Unsupported mediaid")
        }
        return mDatabaseClient.changesObservable
                .startWith(UpnpFolderChange(folderId))
                .filter { it is UpnpFolderChange && folderId == it.folderId }
                .observeOn(AppSchedulers.diskIo)
                .flatMapSingle {
                    Observable.concat(
                            mDatabaseClient.getUpnpFolders(folderId),
                            mDatabaseClient.getUpnpVideos(folderId)
                    ).map { it.toMediaItem() }.toList()
                }
    }
}

/**
 * Loader for newly added row in home screen
 */
class NewlyAddedLoader
@Inject constructor(
        private val mDatabaseClient: DatabaseClient
) {
    val observable: Observable<List<MediaBrowser.MediaItem>> by lazy {
        mDatabaseClient.changesObservable
                .filter { it is UpnpFolderChange || it is UpnpVideoChange }
                .map { true }
                .startWith(true)
                .sample(3, TimeUnit.SECONDS)
                .observeOn(AppSchedulers.diskIo)
                .flatMapSingle {
                    mDatabaseClient.getRecentUpnpVideos().map { it.toMediaItem() }.toList()
                }
    }
}