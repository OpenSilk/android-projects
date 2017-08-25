package org.opensilk.video

import io.reactivex.Observable
import io.reactivex.Single
import org.opensilk.media.MediaRef
import org.opensilk.media.StorageDeviceRef
import org.opensilk.media.UpnpDeviceRef
import org.opensilk.media.database.MediaDAO
import org.opensilk.media.database.UpnpDeviceChange
import org.opensilk.media.loader.storage.StorageDeviceLoader
import javax.inject.Inject

/**
 * The Loader for the Media Servers row in the Home Activity
 */
class MediaDeviceLoader
@Inject constructor(
        private val mDatabaseClient: MediaDAO,
        private val mStorageDeviceLoader: StorageDeviceLoader
){
    val observable: Observable<out List<MediaRef>> by lazy {
        mDatabaseClient.changesObservable
                .filter { it is UpnpDeviceChange }
                .map { true }
                .startWith(true)
                .switchMapSingle {
                    Observable.concat<MediaRef>(
                            mDatabaseClient.getUpnpDevices(),
                            mStorageDeviceLoader.storageDevices.flatMapObservable {
                                Observable.fromIterable<MediaRef>(it)
                            }
                    ).toList().subscribeOn(AppSchedulers.diskIo)
                }
    }

    private val doDatabase: Observable<UpnpDeviceRef> by lazy {
        mDatabaseClient.getUpnpDevices()
    }

    private val doStorageReal: Single<List<StorageDeviceRef>> by lazy {
        mStorageDeviceLoader.storageDevices
    }

}