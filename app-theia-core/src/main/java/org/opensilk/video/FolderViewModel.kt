package org.opensilk.video

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import io.reactivex.disposables.CompositeDisposable
import org.opensilk.media.*
import org.opensilk.media.database.MediaDAO
import timber.log.Timber
import javax.inject.Inject

/**
 *
 */
class FolderViewModel
@Inject constructor(
        private val mDatabaseClient: MediaDAO,
        private val mFolderLoader: FoldersLoader
        ) : ViewModel() {
    val mediaTitle = MutableLiveData<String>()
    val folderItems = MutableLiveData<List<MediaRef>>()
    val loadError = MutableLiveData<String>()

    private val mDisposables = CompositeDisposable()

    override fun onCleared() {
        super.onCleared()
        mDisposables.clear()
    }

    fun onMediaId(mediaId: String) {
        Timber.d("onMediaId($mediaId)")
        val mediaRef = parseMediaId(mediaId)
        when (mediaRef) {
            is MediaDeviceId -> {
                subscribeBrowseItems(mediaRef)
                subscribeTitle(mediaRef)
            }
            is MediaContainerId -> {
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
        mDisposables.add(s)
    }

    fun subscribeTitle(mediaId: MediaId) {
        val s = mDatabaseClient.getMediaRef(mediaId)
                .map {
                    when (it) {
                        is MediaDeviceRef -> it.meta.title
                        else -> it.toMediaDescription().title.toString()
                    }
                }
                .subscribe({
                    mediaTitle.postValue(it)
                })
        mDisposables.add(s)
    }

}