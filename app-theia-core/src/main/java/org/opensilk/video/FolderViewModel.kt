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

    fun onMediaId(mediaId: MediaId) {
        Timber.d("onMediaId($mediaId)")
        when (mediaId) {
            is MediaDeviceId -> {
                subscribeBrowseItems(mediaId)
                subscribeTitle(mediaId)
            }
            is FolderId -> {
                subscribeBrowseItems(mediaId)
                subscribeTitle(mediaId)
            }
            else -> TODO("Unsupported mediaId $mediaId")
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