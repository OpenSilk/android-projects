package org.opensilk.video

import android.arch.lifecycle.*
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposables
import io.reactivex.functions.Consumer
import org.opensilk.media.*
import org.opensilk.media.database.MediaDAO
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

/**
 * ViewModel for the folder screen
 */
class FolderViewModel @Inject constructor(
        private val mDatabaseClient: MediaDAO,
        private val mFolderLoader: FoldersLoader,
        private val mPrefetchLoader: FolderPrefetchLoader
) : ViewModel(), LifecycleObserver {

    val mediaTitle = MutableLiveData<String>()
    val folderItems = MutableLiveData<List<MediaRef>>()
    val loadError = MutableLiveData<String>()

    private var mMediaId: MediaId = NoMediaId
    private var mPrefetchDisposable = Disposables.disposed()
    private val mDisposables = CompositeDisposable()
    private val mChildrenPrefetchOnce = Once()

    override fun onCleared() {
        super.onCleared()
        mDisposables.dispose()
    }

    fun setMediaId(mediaId: MediaId) {
        Timber.d("setMediaId($mediaId)")
        mMediaId = mediaId
        mDisposables.clear()
        runPrefetch()
        subscribeChildren()
        subscribeTitle()
    }

    //@OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun runPrefetch() {
        if (mMediaId == NoMediaId) {
            throw IllegalStateException("MediaId not set")
        }
        val mediaId = mMediaId
        //only fetch if previous fetch is completed
        if (mPrefetchDisposable.isDisposed) {
            mDisposables.remove(mPrefetchDisposable)
            mPrefetchDisposable = when (mediaId) {
                is MediaDeviceId -> mPrefetchLoader.prefetch(mediaId, true)
                is FolderId -> mPrefetchLoader.prefetch(mediaId, true)
                else -> TODO("$mediaId")
            }
            mDisposables.add(mPrefetchDisposable)
            mDisposables.add(mPrefetchLoader.errors(Consumer {
                loadError.postValue(it)
            }))
        }
    }

    private fun runChildPrefetch(items: List<MediaRef>) {
        if (items.isNotEmpty()) {
            //only do it once to prevent infinite loop
            mChildrenPrefetchOnce.Do {
                items.filter { it !is VideoRef }.forEach { c ->
                    mDisposables.add(when (c) {
                        is MediaDeviceRef -> mPrefetchLoader.prefetch(c.id)
                        is FolderRef -> mPrefetchLoader.prefetch(c.id)
                        else -> TODO("$c")
                    })
                }
            }
        }
    }

    private val isFirstLoad = AtomicBoolean(true)

    private fun subscribeChildren() {
        val mediaId = mMediaId
        mDisposables.add(when (mediaId) {
            is MediaDeviceId -> mFolderLoader.directChildren(mediaId)
            is FolderId -> mFolderLoader.directChildren(mediaId)
            else -> TODO("$mediaId")
        }.subscribe({ items ->
            //ignore the first list if it is empty
            if (!isFirstLoad.compareAndSet(true, false) || items.isNotEmpty()) {
                folderItems.postValue(items)
                runChildPrefetch(items)
            }
        }, { e ->
            Timber.e(e, "Loader error msg=${e.message}.")
            loadError.postValue(e.message.elseIfBlank("null"))
        }))
    }

    private fun subscribeTitle() {
        val mediaId = mMediaId
        mDisposables.add(mDatabaseClient.getMediaRef(mediaId).map {
            it.toMediaDescription().title.toString()
        }.subscribe({
            mediaTitle.postValue(it)
        }))
    }

}