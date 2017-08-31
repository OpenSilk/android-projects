package org.opensilk.video

import io.reactivex.Observable
import io.reactivex.Single
import org.opensilk.media.*
import org.opensilk.media.database.DeviceChange
import org.opensilk.media.database.MediaDAO
import javax.inject.Inject

/**
 * The Loader for the Media Servers row in the Home Activity
 */
class DevicesLoader
@Inject constructor(
        private val mDatabaseClient: MediaDAO
){
    fun devices(includeDocuments: Boolean = false): Observable<out List<MediaDeviceRef>> =
            mDatabaseClient.changesObservable
                    .filter { it is DeviceChange }
                    .startWith(DeviceChange())
                    .switchMapSingle {
                        if (includeDocuments) {
                            concatSingleWithDocuments
                        } else {
                            concatSingle
                        }.subscribeOn(AppSchedulers.diskIo)
                    }

    private val upnpObservable: Observable<UpnpDeviceRef> by lazy {
        mDatabaseClient.getAvailableUpnpDevices()
    }

    private val storageObservable: Observable<StorageDeviceRef> by lazy {
        mDatabaseClient.getAvailableStorageDevices()
    }

    private val docObservable: Observable<DocDeviceRef> by lazy {
        Observable.just(DocTreeDeviceRef, DocFileDeviceRef)
    }

    private val concatSingle: Single<out List<MediaDeviceRef>> by lazy {
        Observable.concat<MediaDeviceRef>(
                upnpObservable,
                storageObservable
        ).toList()
    }

    private val concatSingleWithDocuments: Single<out List<MediaDeviceRef>> by lazy {
        Observable.concat<MediaDeviceRef>(
                upnpObservable,
                storageObservable,
                docObservable
        ).toList()
    }

}