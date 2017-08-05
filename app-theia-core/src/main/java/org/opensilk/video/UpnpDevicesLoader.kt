package org.opensilk.video

import android.media.browse.MediaBrowser
import io.reactivex.Observable
import org.opensilk.media.toMediaItem
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
                .flatMapSingle {
                    mDatabaseClient.getUpnpDevices().map {
                        it.toMediaItem()
                    }.toList().subscribeOn(AppSchedulers.diskIo)
                }
    }
}