package org.opensilk.video.phone

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import io.reactivex.disposables.CompositeDisposable
import org.opensilk.media.UpnpDeviceRef
import org.opensilk.video.MediaDeviceLoader
import javax.inject.Inject

/**
 * Created by drew on 8/15/17.
 */
class HomeScreenViewModel
@Inject constructor(
        private val mServersLoader: MediaDeviceLoader
): ViewModel() {

    val servers = MutableLiveData<List<UpnpDeviceRef>>()
    private val disposables = CompositeDisposable()

    fun fetchData() {
        subscribeServers()
    }

    fun subscribeServers() {
        val s = mServersLoader.observable
                .subscribe({
                    servers.postValue(it)
                })

        disposables.add(s)
    }



    override fun onCleared() {
        super.onCleared()
        disposables.clear()
    }

}