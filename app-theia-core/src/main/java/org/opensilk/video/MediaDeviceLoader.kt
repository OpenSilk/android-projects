package org.opensilk.video

import io.reactivex.Observable
import io.reactivex.Single
import org.opensilk.media.MediaDeviceRef
import org.opensilk.media.StorageDeviceRef
import org.opensilk.media.UpnpDeviceRef
import org.opensilk.media.database.DeviceChange
import org.opensilk.media.database.MediaDAO
import org.opensilk.media.database.UpnpDeviceChange
import javax.inject.Inject

/**
 * The Loader for the Media Servers row in the Home Activity
 */
class MediaDeviceLoader
@Inject constructor(
        private val mDatabaseClient: MediaDAO
){
    fun devices(): Observable<out List<MediaDeviceRef>> =
            mDatabaseClient.changesObservable
                    .filter { it is DeviceChange }
                    .startWith(DeviceChange())
                    .switchMapSingle {
                        concatSingle.subscribeOn(AppSchedulers.diskIo)
                    }

    private val upnpObservable: Observable<UpnpDeviceRef> by lazy {
        mDatabaseClient.getAvailableUpnpDevices()
    }

    private val storageObservable: Observable<StorageDeviceRef> by lazy {
        mDatabaseClient.getAvailableStorageDevices()
    }

    private val concatSingle: Single<out List<MediaDeviceRef>> by lazy {
        Observable.concat<MediaDeviceRef>(
                upnpObservable,
                storageObservable
        ).toList()
    }

}