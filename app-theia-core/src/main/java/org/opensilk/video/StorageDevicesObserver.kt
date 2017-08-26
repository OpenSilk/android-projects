package org.opensilk.video

import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleObserver
import android.arch.lifecycle.OnLifecycleEvent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import io.reactivex.Completable
import io.reactivex.disposables.Disposables
import org.opensilk.dagger2.ForApp
import org.opensilk.media.database.MediaDAO
import org.opensilk.media.database.StorageDeviceChange
import org.opensilk.media.loader.storage.StorageDeviceLoader
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Created by drew on 8/26/17.
 */
@Singleton
class StorageDevicesObserver @Inject constructor(
        @ForApp private val mContext: Context,
        private val mStorageDeviceLoader: StorageDeviceLoader,
        private val mDatabaseClient: MediaDAO
): BroadcastReceiver(), LifecycleObserver {

    var mDisposable = Disposables.disposed()

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) {
            return
        }
        Timber.d("onReceive($intent)")
        updateStorageDevices()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    internal fun registerSelf() {
        updateStorageDevices()

        val filter = IntentFilter()
        filter.addAction(Intent.ACTION_MEDIA_MOUNTED)
        filter.addAction(Intent.ACTION_MEDIA_UNMOUNTED)
        filter.addAction(Intent.ACTION_MEDIA_REMOVED)
        filter.addAction(Intent.ACTION_MEDIA_BAD_REMOVAL)
        filter.addAction(Intent.ACTION_MEDIA_SHARED)
        //filter.addAction(Intent.ACTION_MEDIA_EJECT) TODO
        mContext.registerReceiver(this, filter)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    internal fun unregisterSelf() {
        mContext.unregisterReceiver(this)
    }

    fun updateStorageDevices() {
        mDisposable.dispose()
        mDisposable = storageCompletable.subscribeOn(AppSchedulers.diskIo).subscribe({
            mDatabaseClient.postChange(StorageDeviceChange())
        })
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

}