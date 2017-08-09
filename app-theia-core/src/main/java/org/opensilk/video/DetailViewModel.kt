package org.opensilk.video

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.net.Uri
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.BiFunction
import io.reactivex.functions.Consumer
import org.opensilk.common.rx.subscribeIgnoreError
import org.opensilk.media.*
import org.opensilk.tmdb.api.model.TvEpisode
import timber.log.Timber
import javax.inject.Inject
import kotlin.properties.Delegates

/**
 *
 */
class DetailViewModel
@Inject constructor(
        private val mClient: DatabaseClient,
        private val mTVLookup: LookupTVDb,
        private val mMovieLookup: LookupMovieDb
): ViewModel() {
    val videoDescription = MutableLiveData<VideoDescInfo>()
    val fileInfo = MutableLiveData<VideoFileInfo>()
    val resumeInfo = MutableLiveData<ResumeInfo>()
    val posterUri = MutableLiveData<Uri>()
    val backdropUri = MutableLiveData<Uri>()
    val hasDescription = MutableLiveData<Boolean>()
    var mediaRef: MediaRef = NoMediaRef
    val lookupError = MutableLiveData<String>()

    private val disponables = CompositeDisposable()

    fun setMediaId(mediaId: MediaId) {
        disponables.clear()
        when (mediaId) {
            is UpnpVideoId -> {
                subscribeUpnpVideoRef(mediaId)
            }
            else -> TODO()
        }
    }

    private fun subscribeUpnpVideoRef(mediaId: UpnpVideoId) {
        //fetch mediaref
        val o = mClient.changesObservable
                .filter { it is UpnpVideoChange && it.videoId == mediaId }
                .map { true }
                .startWith(true)
                .flatMapMaybe {
                    mClient.getUpnpVideo(mediaId).subscribeOn(AppSchedulers.diskIo)
                }
                .publish()
        //mediaref
        disponables.add(o.subscribeIgnoreError(Consumer {
            mediaRef = it
            hasDescription.postValue(it.tvEpisodeId != null || it.movieId != null)
        }))
        //overview
        disponables.add(o.flatMapMaybe { meta ->
            mClient.getMediaOverview(mediaId).defaultIfEmpty("").map { overview ->
                VideoDescInfo(meta.meta.title.elseIfBlank(meta.meta.mediaTitle), meta.meta.subtitle, overview)
            }.subscribeOn(AppSchedulers.diskIo)
        }.subscribeIgnoreError(Consumer {
            videoDescription.postValue(it)
        }))
        //fileinfo
        disponables.add(o.map { meta ->
            VideoFileInfo(meta.meta.mediaTitle, meta.meta.size, meta.meta.duration)
        }.subscribeIgnoreError(Consumer {
            fileInfo.postValue(it)
        }))
        //lastPosition
        disponables.add(o.flatMapMaybe {
            Maybe.zip<Long, Int, ResumeInfo>(
                    mClient.getLastPlaybackPosition(mediaId),
                    mClient.getLastPlaybackCompletion(mediaId),
                    BiFunction { pos, comp -> ResumeInfo(pos, comp) }
            ).defaultIfEmpty(ResumeInfo()).subscribeOn(AppSchedulers.diskIo)
        }.subscribeIgnoreError(Consumer {
            resumeInfo.postValue(it)
        }))
        //poster
        disponables.add(o.map { it.meta.artworkUri }.filter { it != Uri.EMPTY }
                .subscribeIgnoreError(Consumer {
                    posterUri.postValue(it)
                }))
        //backdrop
        disponables.add(o.map { it.meta.backdropUri }.filter { it != Uri.EMPTY }
                .subscribeIgnoreError(Consumer {
                    backdropUri.postValue(it)
                }))
        //connect after all listeners registered
        disponables.add(o.connect())
    }

    fun doLookup() {
        if (mediaRef == NoMediaRef) {
            return
        }
        val ref = mediaRef
        when (ref) {
            is UpnpVideoRef -> {
                subscribeUpnpVideoLookup(ref)
            }
            else -> TODO()
        }
    }

    private fun subscribeUpnpVideoLookup(mediaRef: UpnpVideoRef) {
        val title = mediaRef.meta.mediaTitle
        if (matchesTvEpisode(title)) {
            val name = extractSeriesName(title)
            val seasonNum = extractSeasonNumber(title)
            val episodeNum = extractEpisodeNumber(title)
            if (name.isNullOrBlank() || seasonNum < 0 || episodeNum < 0) {
                lookupError.postValue("Unable to parse $title as TV episode")
                return
            }
            subscribeTvLookup(LookupRequest(mediaRef = mediaRef, lookupName = name,
                    seasonNumber = seasonNum, episodeNumber = episodeNum))
        } else if (matchesMovie(title)) {
            val name = extractMovieName(title)
            val year = extractMovieYear(title)
            if (name.isNullOrBlank()) {
                lookupError.postValue("Unable to parse $title as movie name")
            }
            subscribeMovieLookup(LookupRequest(mediaRef = mediaRef, lookupName = name,
                    releaseYear = year))
        } else {
            lookupError.postValue("$title does not match movie or tv episode pattern")
        }

    }

    private fun subscribeTvLookup(lookupRequest: LookupRequest) {
        val s = mTVLookup.lookupObservable(lookupRequest)
                .firstOrError()
                .subscribeOn(AppSchedulers.networkIo)
                .subscribe({ epiMeta ->
                    val ref = lookupRequest.mediaRef
                    when (ref) {
                        is UpnpVideoRef -> {
                            Timber.d("Located tv episode ${epiMeta.meta.title} for ${ref.meta.mediaTitle}")
                            mClient.setUpnpVideoTvEpisodeId(ref.id, epiMeta.id)
                            mClient.postChange(UpnpVideoChange(ref.id))
                            mClient.postChange(UpnpFolderChange(ref.parentId))
                            mClient.scheduleRelatedLookup(ref.id)
                        }
                        else -> TODO()
                    }
                }, { err ->
                    val mesg = if (err.message.isNullOrBlank()) err::class.simpleName else err.message
                    Timber.w(err, "Error during lookup $mesg")
                    lookupError.postValue("Error during lookup $mesg")
                })
        disponables.add(s)
    }

    private fun subscribeMovieLookup(lookupRequest: LookupRequest) {
        val s = mMovieLookup.lookupObservable(lookupRequest)
                .firstOrError()
                .subscribeOn(AppSchedulers.networkIo)
                .subscribe({ movieMeta ->
                    val ref = lookupRequest.mediaRef
                    when (ref) {
                        is UpnpVideoRef -> {
                            Timber.d("Located movie ${movieMeta.meta.title} for ${ref.meta.mediaTitle}")
                            mClient.setUpnpVideoMovieId(ref.id, movieMeta.id)
                            mClient.postChange(UpnpVideoChange(ref.id))
                            mClient.postChange(UpnpFolderChange(ref.parentId))
                        }
                        else -> TODO()
                    }
                }, { err ->
                    val mesg = if (err.message.isNullOrBlank()) err::class.simpleName else err.message
                    Timber.w(err, "Error during lookup $mesg")
                    lookupError.postValue("Error during lookup $mesg")
                })
        disponables.add(s)
    }

    override fun onCleared() {
        super.onCleared()
        disponables.clear()
    }
}