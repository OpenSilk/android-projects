package org.opensilk.video

import android.Manifest
import android.arch.lifecycle.*
import android.content.Context
import android.content.pm.PackageManager
import android.support.v4.content.ContextCompat
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.disposables.Disposables
import io.reactivex.exceptions.Exceptions
import org.opensilk.dagger2.ForApp
import org.opensilk.media.MediaRef
import org.opensilk.media.UpnpVideoRef
import javax.inject.Inject

/**
 *
 */
class HomeViewModel
@Inject constructor(
        @ForApp private val mContext: Context,
        private val mServersLoader: MediaDeviceLoader,
        private val mNewlyAddedLoader: NewlyAddedLoader
): ViewModel(), LifecycleObserver {
    val servers = MutableLiveData<List<MediaRef>>()
    val newlyAdded = MutableLiveData<List<UpnpVideoRef>>()
    val needPermissions = MutableLiveData<Array<String>>()

    private val mDisposables = CompositeDisposable()
    private var mServersSubscription = Disposables.disposed()

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    internal fun checkPermissions() {
        val perms = ContextCompat.checkSelfPermission(mContext, Manifest.permission.READ_EXTERNAL_STORAGE)
        if (perms != PackageManager.PERMISSION_GRANTED) {
            needPermissions.postValue(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE))
        }
    }

    fun onGrantedPermissions(perms: List<String>) {
        if (perms.contains(Manifest.permission.READ_EXTERNAL_STORAGE)) {
            subscribeServers()
        }
    }

    fun fetchData() {
        subscribeServers()
        subscribeNewlyAdded()
    }

    fun subscribeServers() {
        mDisposables.remove(mServersSubscription)
        mServersSubscription = mServersLoader.observable
                .subscribe({
                    servers.postValue(it)
                }, {
                    Exceptions.propagate(it) //TODO handle errors
                }
        )
        mDisposables.add(mServersSubscription)
    }

    private fun subscribeNewlyAdded() {
        val s = mNewlyAddedLoader.observable
                .subscribe({
                    newlyAdded.postValue(it)
                }, {
                    Exceptions.propagate(it)
                })
        mDisposables.add(s)
    }

    override fun onCleared() {
        super.onCleared()
        mDisposables.dispose()
    }

}