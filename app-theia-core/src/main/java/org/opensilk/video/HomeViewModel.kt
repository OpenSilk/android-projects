package org.opensilk.video

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.media.browse.MediaBrowser
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.exceptions.Exceptions
import org.opensilk.media.toMediaItem
import javax.inject.Inject

/**
 *
 */
class HomeViewModel
@Inject constructor(
        private val mServersLoader: UpnpDevicesLoader,
        private val mNewlyAddedLoader: NewlyAddedLoader
): ViewModel() {
    val servers = MutableLiveData<List<MediaBrowser.MediaItem>>()
    val newlyAdded = MutableLiveData<List<MediaBrowser.MediaItem>>()
    private val disposables = CompositeDisposable()

    fun fetchData() {
        subscribeServers()
        subscribeNewlyAdded()
    }

    fun subscribeServers() {
        val s = mServersLoader.observable
                .map { list -> list.map { it.toMediaItem() } }
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
                .map { list -> list.map { it.toMediaItem() } }
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