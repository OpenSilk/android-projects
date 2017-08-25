package org.opensilk.video

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.content.Context
import android.net.Uri
import io.reactivex.Maybe
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.BiFunction
import io.reactivex.functions.Consumer
import org.opensilk.media.*
import org.opensilk.media.database.MediaDAO
import org.opensilk.media.database.VideoChange
import org.opensilk.reactivex2.subscribeIgnoreError
import timber.log.Timber
import javax.inject.Inject

/**
 *
 */
class DetailViewModel
@Inject constructor(
        private val mClient: MediaDAO,
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

    private val mDisposables = CompositeDisposable()

    override fun onCleared() {
        super.onCleared()
        mDisposables.dispose()
    }

    fun setMediaId(mediaId: MediaId) {
        mDisposables.clear()
        when (mediaId) {
            is VideoId -> {
                subscribeVideoRef(mediaId)
            }
            else -> TODO()
        }
    }

    private fun subscribeVideoRef(mediaId: VideoId) {
        //fetch mediaref
        val o = mClient.changesObservable
                .filter { it is VideoChange && it.videoId == mediaId }
                .map { true }
                .startWith(true)
                .flatMapMaybe {
                    mClient.getMediaRef(mediaId)
                            .map { it as VideoRef }
                            .subscribeOn(AppSchedulers.diskIo)
                }
                .publish()
        //mediaref
        mDisposables.add(o.subscribeIgnoreError(Consumer {
            mediaRef = it
            hasDescription.postValue(it.tvEpisodeId != null || it.movieId != null)
        }))
        //overview
        mDisposables.add(o.flatMapMaybe { meta ->
            mClient.getVideoOverview(meta.id).defaultIfEmpty("").map { overview ->
                VideoDescInfo(meta.meta.title, meta.meta.subtitle, overview)
            }.subscribeOn(AppSchedulers.diskIo)
        }.subscribeIgnoreError(Consumer {
            videoDescription.postValue(it)
        }))
        //fileinfo
        mDisposables.add(o.map { meta ->
            VideoFileInfo(meta.meta.originalTitle.elseIfBlank(meta.meta.title), meta.meta.size, meta.meta.duration)
        }.subscribeIgnoreError(Consumer {
            fileInfo.postValue(it)
        }))
        //lastPosition
        mDisposables.add(o.flatMapMaybe { meta ->
            Maybe.zip<Long, Int, ResumeInfo>(
                    mClient.getLastPlaybackPosition(meta.id),
                    mClient.getLastPlaybackCompletion(meta.id),
                    BiFunction { pos, comp -> ResumeInfo(pos, comp) }
            ).defaultIfEmpty(ResumeInfo()).subscribeOn(AppSchedulers.diskIo)
        }.subscribeIgnoreError(Consumer {
            resumeInfo.postValue(it)
        }))
        //poster
        mDisposables.add(o.map { it.meta.artworkUri }.filter { it != Uri.EMPTY }
                .subscribeIgnoreError(Consumer {
                    posterUri.postValue(it)
                }))
        //backdrop
        mDisposables.add(o.map { it.meta.backdropUri }.filter { it != Uri.EMPTY }
                .subscribeIgnoreError(Consumer {
                    backdropUri.postValue(it)
                }))
        //connect after all listeners registered
        mDisposables.add(o.connect())
    }

    fun doLookup(context: Context) {
        if (mediaRef == NoMediaRef) {
            return
        }
        val ref = mediaRef
        when (ref) {
            is VideoRef -> {
                subscribeLookup(context, ref, ref.meta.originalTitle.elseIfBlank(ref.meta.title))
            }
            else -> TODO()
        }
    }

    private fun subscribeLookup(context: Context, mediaRef: MediaRef, title: String) {
        when {
            matchesTvEpisode(title) -> {
                val name = extractSeriesName(title)
                val seasonNum = extractSeasonNumber(title)
                val episodeNum = extractEpisodeNumber(title)
                if (name.isBlank() || seasonNum < 0 || episodeNum < 0) {
                    lookupError.postValue("Unable to parse $title as TV episode")
                    return
                }
                subscribeTvLookup(context, LookupRequest(mediaRef = mediaRef, lookupName = name,
                        seasonNumber = seasonNum, episodeNumber = episodeNum))
            }
            matchesMovie(title) -> {
                val name = extractMovieName(title)
                val year = extractMovieYear(title)
                if (name.isBlank()) {
                    lookupError.postValue("Unable to parse $title as movie name")
                }
                subscribeMovieLookup(LookupRequest(mediaRef = mediaRef, lookupName = name,
                        releaseYear = year))
            }
            else -> lookupError.postValue("$title does not match movie or tv episode pattern")
        }

    }

    private fun subscribeTvLookup(context: Context, lookupRequest: LookupRequest) {
        val s = mTVLookup.lookupObservable(lookupRequest)
                .firstOrError()
                .subscribeOn(AppSchedulers.networkIo)
                .subscribe({ epiMeta ->
                    val ref = lookupRequest.mediaRef
                    when (ref) {
                        is VideoRef -> {
                            Timber.d("Located tv episode ${epiMeta.meta.title} for ${ref.meta.title}")
                            mClient.setVideoTvEpisodeId(ref.id, epiMeta.id)
                            context.scheduleRelatedLookup(ref.id) //TODO import a jobscheduler helper
                        }
                        else -> TODO()
                    }
                }, { err ->
                    val mesg = if (err.message.isNullOrBlank()) err::javaClass.name else err.message
                    Timber.w(err, "Error during lookup $mesg")
                    lookupError.postValue("Error during lookup $mesg")
                })
        mDisposables.add(s)
    }

    private fun subscribeMovieLookup(lookupRequest: LookupRequest) {
        val s = mMovieLookup.lookupObservable(lookupRequest)
                .firstOrError()
                .subscribeOn(AppSchedulers.networkIo)
                .subscribe({ movieMeta ->
                    val ref = lookupRequest.mediaRef
                    when (ref) {
                        is VideoRef -> {
                            Timber.d("Located movie ${movieMeta.meta.title} for ${ref.meta.title}")
                            mClient.setVideoMovieId(ref.id, movieMeta.id)
                        }
                        else -> TODO()
                    }
                }, { err ->
                    val mesg = if (err.message.isNullOrBlank()) err::javaClass.name else err.message
                    Timber.w(err, "Error during lookup $mesg")
                    lookupError.postValue("Error during lookup $mesg")
                })
        mDisposables.add(s)
    }

}