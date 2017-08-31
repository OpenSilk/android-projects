package org.opensilk.video

import android.Manifest
import android.arch.lifecycle.*
import android.content.Context
import android.content.pm.PackageManager
import android.support.v4.content.ContextCompat
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.exceptions.Exceptions
import org.opensilk.dagger2.ForApp
import org.opensilk.media.MediaDeviceRef
import org.opensilk.media.UpnpVideoRef
import org.opensilk.media.VideoRef
import javax.inject.Inject

/**
 *
 */
class HomeViewModel
@Inject constructor(
        @ForApp private val mContext: Context,
        private val mServersLoader: DevicesLoader,
        private val mRecentsLoader: RecentlyPlayedLoader,
        private val mStorageObserver: StorageDevicesObserver
): ViewModel(), LifecycleObserver {

    val devices = MutableLiveData<List<MediaDeviceRef>>()
    val recentlyPlayed = MutableLiveData<List<VideoRef>>()
    val needPermissions = MutableLiveData<Array<String>>()

    private val mDisposables = CompositeDisposable()
    private val mSubscribeOnce = Once()

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    internal fun checkPermissions() {
        val perms = ContextCompat.checkSelfPermission(mContext, Manifest.permission.READ_EXTERNAL_STORAGE)
        if (perms != PackageManager.PERMISSION_GRANTED) {
            needPermissions.postValue(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE))
        }
    }

    fun onGrantedPermissions(perms: List<String>) {
        if (perms.contains(Manifest.permission.READ_EXTERNAL_STORAGE)) {
            mStorageObserver.updateStorageDevices()
        }
    }

    fun subscribeData(includeDocuments: Boolean = false) {
        mSubscribeOnce.Do {
            subscribeServers(includeDocuments)
            subscribeRecents()
        }
    }

    private var firstList = true

    private fun subscribeServers(includeDocuments: Boolean) {
        val s = mServersLoader.devices(includeDocuments)
                .subscribe({ list ->
                    //first list might be empty since it comes from the database
                    //don't post it, so the ui wont display empty
                    if (!firstList || list.isNotEmpty()) {
                        firstList = false
                        devices.postValue(list)
                    }
                }, {
                    Exceptions.propagate(it) //TODO handle errors
                }
        )
        mDisposables.add(s)
    }

    private fun subscribeRecents() {
        val s = mRecentsLoader.recentlyPlayed()
                .subscribe({
                    recentlyPlayed.postValue(it)
                })
        mDisposables.add(s)
    }

    override fun onCleared() {
        super.onCleared()
        mDisposables.dispose()
    }

}