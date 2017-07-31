package org.opensilk.video

import android.media.browse.MediaBrowser
import dagger.Binds
import dagger.Module
import org.opensilk.media.*
import rx.Observable
import javax.inject.Inject

/**
 * This module is superseded in mock for espresso tests
 */
@Module
abstract class UpnpLoadersModule {
    @Binds
    abstract fun provideCDSDevicesLoader(impl: UpnpDevicesLoaderImpl): UpnpDevicesLoader
    @Binds
    abstract fun provideCDSBrowseLoader(impl: UpnpFoldersLoaderImpl): UpnpFoldersLoader
}

/**
 * The Loader for the Media Servers row in the Home Activity
 */
interface UpnpDevicesLoader {
    val observable: Observable<List<MediaBrowser.MediaItem>>
}

/**
 * Created by drew on 5/29/17.
 */
class UpnpDevicesLoaderImpl
@Inject constructor(
        private val mDatabaseClient: DatabaseClient
): UpnpDevicesLoader {

    override val observable: Observable<List<MediaBrowser.MediaItem>> by lazy {
        mDatabaseClient.changesObservable
                .filter { it is UpnpDeviceChange }
                .map { it as UpnpDeviceChange }
                .startWith(UpnpDeviceChange())
                .flatMap {
                    mDatabaseClient.getUpnpDevices()
                            .map { it.toMediaItem() }
                            .toList().subscribeOn(AppSchedulers.diskIo)
                }
    }
}

/**
 * The loader for the folder activity
 */
interface UpnpFoldersLoader {
    fun observable(mediaId: String): Observable<List<MediaBrowser.MediaItem>>
}

/**
 *
 */
class UpnpFoldersLoaderImpl
@Inject constructor(
        private val mDatabaseClient: DatabaseClient
) : UpnpFoldersLoader {
    override fun observable(mediaId: String): Observable<List<MediaBrowser.MediaItem>> {
        val mediaRef = newMediaRef(mediaId)
        val folderId = when (mediaRef.kind) {
            UPNP_FOLDER -> mediaRef.mediaId as UpnpFolderId
            UPNP_DEVICE -> UpnpFolderId((mediaRef.mediaId as UpnpDeviceId).deviceId, "0")
            else -> TODO("Unsupported mediaid")
        }
        return mDatabaseClient.changesObservable
                .filter { it is UpnpFolderChange && folderId == it.folderId }
                .flatMap {
                    Observable.concat(
                            mDatabaseClient.getUpnpFolders(folderId).subscribeOn(AppSchedulers.diskIo),
                            mDatabaseClient.getUpnpVideos(folderId).subscribeOn(AppSchedulers.diskIo)
                    ).map { it.toMediaItem() }.toList()
                }
    }
}