package org.opensilk.video

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.exceptions.Exceptions
import org.opensilk.media.UpnpDeviceRef
import org.opensilk.media.UpnpVideoRef
import javax.inject.Inject

/**
 *
 */
class HomeViewModel
@Inject constructor(
        private val mServersLoader: UpnpDevicesLoader,
        private val mNewlyAddedLoader: NewlyAddedLoader
): ViewModel() {
    val servers = MutableLiveData<List<UpnpDeviceRef>>()
    val newlyAdded = MutableLiveData<List<UpnpVideoRef>>()
    private val disposables = CompositeDisposable()

    fun fetchData() {
        subscribeServers()
        subscribeNewlyAdded()
    }

    fun subscribeServers() {
        val s = mServersLoader.observable
                .subscribe({
                    servers.postValue(it)
                }, {
                    Exceptions.propagate(it) //TODO handle errors
                }
        )
        disposables.add(s)
    }

    fun subscribeNewlyAdded() {
        val s = mNewlyAddedLoader.observable
                .subscribe({
                    newlyAdded.postValue(it)
                }, {
                    Exceptions.propagate(it)
                })
        disposables.add(s)
    }

    override fun onCleared() {
        super.onCleared()
        disposables.clear()
    }

}