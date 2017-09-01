package org.opensilk.video

import io.reactivex.Observable
import io.reactivex.Single
import org.opensilk.media.FolderRef
import org.opensilk.media.MediaDeviceRef
import org.opensilk.media.MediaRef
import org.opensilk.media.database.DeviceChange
import org.opensilk.media.database.FolderChange
import org.opensilk.media.database.MediaDAO
import javax.inject.Inject

/**
 * Created by drew on 9/1/17.
 */
class PinnedLoader
@Inject constructor(
        private val mDatabaseClient: MediaDAO
){

    fun pinnedContainers(): Observable<List<MediaRef>> =
            mDatabaseClient.changesObservable
                    .filter { it is DeviceChange || it is FolderChange }
                    .startWith(DeviceChange())
                    .switchMapSingle {
                        pinnedSingle.subscribeOn(AppSchedulers.diskIo)
                    }

    private val pinnedSingle: Single<List<MediaRef>> by lazy {
        mDatabaseClient.getPinnedItems().filter {
            it is MediaDeviceRef || it is FolderRef
        }.toList()
    }

}