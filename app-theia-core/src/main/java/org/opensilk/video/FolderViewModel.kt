package org.opensilk.video

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import io.reactivex.disposables.CompositeDisposable
import org.opensilk.media.*
import org.opensilk.media.playback.MediaProviderClient
import timber.log.Timber
import javax.inject.Inject

/**
 *
 */
class FolderViewModel
@Inject constructor(
        private val mDatabaseClient: MediaProviderClient,
        private val mFolderLoader: FoldersLoader
        ) : ViewModel() {
    val mediaTitle = MutableLiveData<String>()
    val folderItems = MutableLiveData<List<MediaRef>>()
    val loadError = MutableLiveData<String>()
    private val disposables = CompositeDisposable()

    fun onMediaId(mediaId: String) {
        Timber.d("onMediaId($mediaId)")
        val mediaRef = parseMediaId(mediaId)
        when (mediaRef) {
            is UpnpFolderId,
            is UpnpDeviceId -> {
                subscribeBrowseItems(mediaRef)
                subscribeTitle(mediaRef)
            }
            is DocumentId -> {
                if (!mediaRef.isFromTree) {
                    TODO("Document must be for tree $mediaRef")
                }
                subscribeBrowseItems(mediaRef)
                subscribeTitle(mediaRef)
            }
            else -> TODO("Unsupported mediaId $mediaRef")
        }
    }

    fun subscribeBrowseItems(mediaId: MediaId) {
        val s = mFolderLoader.observable(mediaId)
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
                .map({ it.toMediaDescription() })
                .subscribe({
                    mediaTitle.postValue(it.title?.toString())
                })
        disposables.add(s)
    }

    override fun onCleared() {
        super.onCleared()
        disposables.clear()
    }
}