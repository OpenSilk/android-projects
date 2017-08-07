package org.opensilk.video

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.net.Uri
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.BiFunction
import io.reactivex.functions.Consumer
import org.opensilk.common.rx.subscribeIgnoreError
import org.opensilk.media.UpnpVideoId
import org.opensilk.media.UpnpVideoRef
import org.opensilk.media.elseIfBlank
import org.opensilk.media.parseMediaId
import javax.inject.Inject

/**
 *
 */
class DetailViewModel
@Inject constructor(
        val mClient: DatabaseClient
): ViewModel() {
    val videoDescription = MutableLiveData<VideoDescInfo>()
    val fileInfo = MutableLiveData<VideoFileInfo>()
    val resumeInfo = MutableLiveData<ResumeInfo>()
    val posterUri = MutableLiveData<Uri>()
    val backdropUri = MutableLiveData<Uri>()

    private val disponables = CompositeDisposable()

    fun onMediaId(mediaId: String) {
        disponables.clear()
        val ref = parseMediaId(mediaId)
        when (ref) {
            is UpnpVideoId -> {
                subscribeVideoDescription(ref)
                subscribeFileInfo(ref)
                subscribeLastPosition(ref)
                subscribePosterUri(ref)
                subscribeBackdropUri(ref)
            }
        }
    }

    fun changes(mediaId: UpnpVideoId): Observable<Boolean> {
        return mClient.changesObservable
                .filter { it is UpnpVideoChange && it.videoId == mediaId }
                .map { true }
                .startWith(true)
    }

    fun cachedMeta(mediaId: UpnpVideoId): Observable<UpnpVideoRef> {
        return changes(mediaId)
                .flatMapMaybe {
                    mClient.getUpnpVideo(mediaId).subscribeOn(AppSchedulers.diskIo)
                }
    }

    fun subscribeVideoDescription(mediaId: UpnpVideoId) {
        val s = cachedMeta(mediaId).flatMapMaybe { meta ->
            mClient.getMediaOverview(mediaId).defaultIfEmpty("").map { overview ->
                VideoDescInfo(meta.meta.title.elseIfBlank(meta.meta.mediaTitle), meta.meta.subtitle, overview)
            }
        }.subscribeIgnoreError(Consumer {
            videoDescription.postValue(it)
        })
        disponables.add(s)
    }

    fun subscribeFileInfo(mediaId: UpnpVideoId) {
        val s = cachedMeta(mediaId).map { meta ->
            VideoFileInfo(meta.meta.mediaTitle, meta.meta.size, meta.meta.duration)
        }.subscribeIgnoreError(Consumer {
            fileInfo.postValue(it)
        })
        disponables.add(s)
    }

    fun subscribeLastPosition(mediaId: UpnpVideoId) {
        val s = changes(mediaId)
                .flatMapMaybe {
                    Maybe.zip<Long, Int, ResumeInfo>(
                            mClient.getLastPlaybackPosition(mediaId),
                            mClient.getLastPlaybackCompletion(mediaId),
                            BiFunction { pos, comp -> ResumeInfo(pos, comp) }
                    ).defaultIfEmpty(ResumeInfo()).subscribeOn(AppSchedulers.diskIo)
                }
                .subscribeIgnoreError(Consumer {
                    resumeInfo.postValue(it)
                })
        disponables.add(s)
    }

    fun subscribePosterUri(mediaId: UpnpVideoId) {
        val s = cachedMeta(mediaId)
                .map { it.meta.artworkUri }
                .filter { it != Uri.EMPTY }
                .subscribeIgnoreError(Consumer {
                    posterUri.postValue(it)
                })
        disponables.add(s)
    }

    fun subscribeBackdropUri(mediaId: UpnpVideoId) {
        val s = cachedMeta(mediaId)
                .map { it.meta.backdropUri }
                .filter { it != Uri.EMPTY }
                .subscribeIgnoreError(Consumer {
                    backdropUri.postValue(it)
                })
        disponables.add(s)
    }

    override fun onCleared() {
        super.onCleared()
        disponables.clear()
    }
}