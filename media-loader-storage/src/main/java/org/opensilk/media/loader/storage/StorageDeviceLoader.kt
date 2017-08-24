package org.opensilk.media.loader.storage

import io.reactivex.Single
import org.opensilk.media.StorageDeviceRef

/**
 * Created by drew on 8/20/17.
 */
interface StorageDeviceLoader {
    val storageDevices: Single<List<StorageDeviceRef>>
}