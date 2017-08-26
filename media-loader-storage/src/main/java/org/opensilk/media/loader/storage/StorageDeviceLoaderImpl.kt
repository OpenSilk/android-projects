package org.opensilk.media.loader.storage

import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build
import android.os.Environment
import android.os.storage.StorageManager
import android.os.storage.StorageVolume
import android.support.v4.content.ContextCompat
import io.reactivex.Maybe
import io.reactivex.Single
import org.opensilk.dagger2.ForApp
import org.opensilk.media.*
import timber.log.Timber
import java.io.File
import javax.inject.Inject

/**
 * Created by drew on 8/17/17.
 */
class StorageDeviceLoaderImpl
@Inject constructor(
        @ForApp private val mContext: Context
): StorageDeviceLoader {

    override val storageDevices: Single<List<StorageDeviceRef>> = Single.defer {
        when {
            Build.VERSION.SDK_INT >= 24 && mContext.hasAccessActivity() -> TODO()
            //on android tv and devices < api24 read permission grants access to all storage
            mContext.canReadPrimaryStorage() -> devicesReadValidated
            else -> Single.just(emptyList())
        }
    }

    private val fallbackStorageDevice: List<StorageDeviceRef> by lazy {
        listOf(StorageDeviceRef(
                StorageDeviceId(
                        uuid = suitableFakeUuid(true, Environment.isExternalStorageEmulated(),
                                Environment.isExternalStorageRemovable()),
                        path = Environment.getExternalStorageDirectory().path,
                        isPrimary = true
                ),
                StorageDeviceMeta(
                        title = "Primary"
                )
        ))
    }

    private val devicesReadValidated: Single<List<StorageDeviceRef>> by lazy {
        Maybe.create<List<StorageDeviceRef>> { s ->
            val list = StorageManagerAccessor().volumeList()
            if (list.isEmpty()) {
                s.onComplete()
            } else {
                s.onSuccess(list)
            }
        }.defaultIfEmpty(fallbackStorageDevice).onErrorReturnItem(emptyList()).toSingle()
    }

    private val mountedStates = arrayOf(
            Environment.MEDIA_MOUNTED,
            Environment.MEDIA_MOUNTED_READ_ONLY
    )

    inner class StorageManagerAccessor {
        fun volumeList(): List<StorageDeviceRef> = if (Build.VERSION.SDK_INT >= 24) api24() else api21()

        @TargetApi(24)
        private fun api24(): List<StorageDeviceRef> {
            val storageManager = mContext.getSystemService(StorageManager::class.java)
            val volumes = storageManager.storageVolumes
            val list = ArrayList<StorageDeviceRef>()
            val getPath = StorageVolume::class.java.getDeclaredMethod("getPath")
            getPath.isAccessible = true
            volumes.mapNotNullTo(list) { v ->
                val directory = File(getPath.invoke(v) as String)
                val isMounted = mountedStates.contains(v.state)
                if (!isMounted || !directory.isDirectory || !directory.canRead()) {
                    null
                } else {
                    StorageDeviceRef(StorageDeviceId(
                            uuid = v.uuid ?: suitableFakeUuid(v.isPrimary, v.isEmulated, v.isRemovable),
                            isPrimary = v.isPrimary,
                            path = directory.path
                    ), StorageDeviceMeta(
                            title = v.getDescription(mContext)
                            //intent = if (!v.isPrimary) v.createAccessIntent(null) else EMPTY_INTENT
                    ))
                }
            }
            return list
        }

        private fun api21(): List<StorageDeviceRef> {
            val sm = mContext.getSystemService(Context.STORAGE_SERVICE) as StorageManager
            val getVolumeList = StorageManager::class.java.getDeclaredMethod("getVolumeList")
            val volume = Class.forName("android.os.storage.StorageVolume")
            val getPath = volume.getDeclaredMethod("getPath")
            val getDescription = volume.getDeclaredMethod("getDescription", Context::class.java)
            val getUuid = volume.getDeclaredMethod("getUuid")
            val getState = volume.getDeclaredMethod("getState")
            val getIsPrimary = volume.getDeclaredMethod("isPrimary")
            val getIsRemovable = volume.getDeclaredMethod("isRemovable")
            val getIsEmulated = volume.getDeclaredMethod("isEmulated")
            val volumes = getVolumeList.invoke(sm) as Array<*>
            val list = ArrayList<StorageDeviceRef>()
            volumes.mapNotNullTo(list) { v ->
                val isPrimary = getIsPrimary.invoke(v) as Boolean
                val isRemovable = getIsRemovable.invoke(v) as Boolean
                val isEmulated = getIsEmulated.invoke(v) as Boolean
                val directory = File(getPath.invoke(v) as String)
                val isMounted = mountedStates.contains(getState.invoke(v) as String)
                if (!isMounted || !directory.isDirectory || !directory.canRead()) {
                    null
                } else {
                    StorageDeviceRef(StorageDeviceId(
                            uuid = (getUuid.invoke(v) as? String) ?: suitableFakeUuid(
                                    isPrimary, isEmulated, isRemovable),
                            path = directory.path,
                            isPrimary = isPrimary
                    ), StorageDeviceMeta(
                            title = getDescription.invoke(v, mContext) as String
                    ))
                }
            }
            return list
        }
    }

}