package org.opensilk.video

import io.reactivex.Observable
import io.reactivex.Single
import org.opensilk.media.UpnpDeviceRef
import javax.inject.Inject

/**
 * The Loader for the Media Servers row in the Home Activity
 */
class UpnpDevicesLoader
@Inject constructor(
        private val mDatabaseClient: DatabaseClient
){
    val observable: Observable<List<UpnpDeviceRef>> by lazy {
        mDatabaseClient.changesObservable
                .filter { it is UpnpDeviceChange }
                .map { true }
                .startWith(true)
                .switchMapSingle { doDatabase }
    }

    private val doDatabase: Single<List<UpnpDeviceRef>> by lazy {
        mDatabaseClient.getUpnpDevices().toList().subscribeOn(AppSchedulers.diskIo)
    }
}