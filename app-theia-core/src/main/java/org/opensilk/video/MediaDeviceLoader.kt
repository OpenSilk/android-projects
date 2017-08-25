package org.opensilk.video

import io.reactivex.Completable
import io.reactivex.Observable
import org.opensilk.media.MediaDeviceRef
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
    val observable: Observable<out List<MediaDeviceRef>> by lazy {
        mDatabaseClient.changesObservable
                .filter { it is UpnpDeviceChange }
                .map { true }
                .startWith(true)
                .switchMapSingle {
                    Observable.concat<MediaDeviceRef>(
                            upnpObservable,
                            storageCompletable.andThen(storageObservable)
                    ).toList().subscribeOn(AppSchedulers.diskIo)
                }
    }

    private val upnpObservable: Observable<UpnpDeviceRef> by lazy {
        mDatabaseClient.getAvailableUpnpDevices()
    }

    private val storageCompletable: Completable by lazy {
        mStorageDeviceLoader.storageDevices.flatMapCompletable { deviceList ->
            Completable.fromAction {
                mDatabaseClient.hideAllStorageDevices()
                deviceList.forEach {
                    mDatabaseClient.addStorageDevice(it)
                }
            }
        }
    }

    private val storageObservable: Observable<StorageDeviceRef> by lazy {
        mDatabaseClient.getAvailableStorageDevices()
    }

}