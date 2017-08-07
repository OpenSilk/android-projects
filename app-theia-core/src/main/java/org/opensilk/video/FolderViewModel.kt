package org.opensilk.video

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.media.browse.MediaBrowser
import io.reactivex.disposables.CompositeDisposable
import org.opensilk.media.*
import timber.log.Timber
import javax.inject.Inject

/**
 *
 */
class FolderViewModel
@Inject constructor(
        private val mDatabaseClient: MediaProviderClient,
        private val mBrowseLoader: UpnpFoldersLoader
) : ViewModel() {
    val mediaTitle = MutableLiveData<String>()
    val folderItems = MutableLiveData<List<MediaRef>>()
    val loadError = MutableLiveData<String>()
    private val disposables = CompositeDisposable()

    fun onMediaId(mediaId: String) {
        Timber.d("onMediaId($mediaId)")
        val mediaRef = parseMediaId(mediaId)
        when (mediaRef) {
            is UpnpFolderId, is UpnpDeviceId -> {
                subscribeBrowseItems(mediaRef)
                subscribeTitle(mediaRef)
            }
            else -> TODO("Unsupported mediaId $mediaRef")
        }
    }

    fun subscribeBrowseItems(mediaId: MediaId) {
        val s = mBrowseLoader.observable(mediaId)
                .subscribe({
                    folderItems.postValue(it)
                }, {
                    Timber.e(it, "Loader error msg=${it.message}.")
                    loadError.postValue(it.message.elseIfBlank("null"))
                })
        disposables.add(s)
    }

    fun subscribeTitle(mediaId: MediaId) {
        val s = mDatabaseClient.getMediaMeta(mediaId)
                .map({ it.toMediaItem() })
                .subscribe({
                    mediaTitle.postValue(it.description.title?.toString())
                })
        disposables.add(s)
    }

    override fun onCleared() {
        super.onCleared()
        disposables.clear()
    }
}