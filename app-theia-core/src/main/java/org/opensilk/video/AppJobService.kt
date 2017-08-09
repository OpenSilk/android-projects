package org.opensilk.video

import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import dagger.Module
import dagger.Subcomponent
import io.reactivex.Maybe
import io.reactivex.disposables.Disposables
import org.opensilk.common.dagger.Injector
import org.opensilk.common.dagger.injectMe
import org.opensilk.media.*
import timber.log.Timber
import java.net.SocketTimeoutException
import javax.inject.Inject

const val JOB_RELATED_LOOKUP = 1

@Subcomponent
interface AppJobServiceComponent: Injector<AppJobService> {
    @Subcomponent.Builder
    abstract class Builder: Injector.Builder<AppJobService>()
}

@Module(subcomponents = arrayOf(AppJobServiceComponent::class))
abstract class AppJobServiceModule

/**
 * Created by drew on 7/25/17.
 */
class AppJobService : JobService() {

    @Inject lateinit var mDatabaseClient: DatabaseClient
    @Inject lateinit var mTvLookup: LookupTVDb

    override fun onCreate() {
        super.onCreate()
        injectMe()
    }

    var relatedLookupSub = Disposables.disposed()

    override fun onStopJob(params: JobParameters): Boolean {
        Timber.d("onStopJob($params)")
        return when(params.jobId) {
            JOB_RELATED_LOOKUP -> {
                if (relatedLookupSub.isDisposed) {
                    false
                } else {
                    relatedLookupSub.dispose()
                    true
                }
            }
            else -> false
        }
    }

    override fun onStartJob(params: JobParameters): Boolean {
        Timber.d("onStartJob($params)")
        return when (params.jobId) {
            JOB_RELATED_LOOKUP -> {
                subscribeLookupRelated(params)
            }
            else -> false
        }
    }

    fun subscribeLookupRelated(params: JobParameters): Boolean {
        val ref = parseMediaId(params.extras.getString(EXTRA_MEDIAID))
        when (ref) {
            is UpnpVideoId -> {
                subscribeUpnpVideoLookupRelated(ref, params)
                return true
            }
            else -> return false
        }
    }

    private data class UpnpVideoRefWithEpisode(val mediaRef: UpnpVideoRef, val episodeRef: TvEpisodeRef)

    fun subscribeUpnpVideoLookupRelated(mediaId: UpnpVideoId, params: JobParameters) {
        relatedLookupSub.dispose()
        relatedLookupSub = mDatabaseClient.getRelatedUpnpVideos(mediaId)
                .flatMapMaybe<UpnpVideoRefWithEpisode> { ref ->
                    val title = ref.meta.mediaTitle
                    val name = extractSeriesName(title)
                    val seasonNum = extractSeasonNumber(title)
                    val episodeNum = extractEpisodeNumber(title)
                    if (name.isNullOrBlank() || seasonNum < 0 || episodeNum < 0) {
                        return@flatMapMaybe Maybe.empty()
                    }
                    return@flatMapMaybe mTvLookup.lookupObservable(LookupRequest(mediaRef = ref,
                            lookupName = name, seasonNumber = seasonNum, episodeNumber = episodeNum))
                            .firstElement()
                            .map { UpnpVideoRefWithEpisode(ref, it) }
                }
                .subscribeOn(AppSchedulers.networkIo)
                .subscribe({ vwe ->
                    mDatabaseClient.setUpnpVideoTvEpisodeId(vwe.mediaRef.id, vwe.episodeRef.id)
                    mDatabaseClient.postChange(UpnpFolderChange(vwe.mediaRef.parentId))
                }, { err ->
                    Timber.d(err, "Unsuccessful lookup for videos related to $mediaId")
                    if (err is SocketTimeoutException) {
                        jobFinished(params, true)
                    } else {
                        jobFinished(params, false)
                    }
                }, {
                    jobFinished(params, false)
                })
    }
}