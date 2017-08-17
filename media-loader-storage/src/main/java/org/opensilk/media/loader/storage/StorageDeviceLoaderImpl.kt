package org.opensilk.media.loader.storage

import android.annotation.TargetApi
import android.content.Context
import android.os.Environment
import android.os.storage.StorageManager
import io.reactivex.Single
import org.opensilk.dagger2.ForApp
import org.opensilk.media.StorageDeviceRef
import javax.inject.Inject

/**
 * Created by drew on 8/17/17.
 */
class StorageDeviceLoaderImpl
@Inject constructor(
        @ForApp private val mContext: Context
){

    fun storageVolumes() {

    }

    fun storageVolumesApi21() {
        val dir = Environment.getExternalStorageDirectory()
        val name = when {
            Environment.isExternalStorageEmulated() -> "Internal"
            Environment.isExternalStorageRemovable() -> "SDCard"
            else -> "Primary"
        }
    }

    @TargetApi(24)
    fun storageVolumeApi24(): Single<List<StorageDeviceRef>> {
        val storageManager = mContext.getSystemService(StorageManager::class.java)
        val volumes = storageManager.storageVolumes
        volumes[0]
    }

}