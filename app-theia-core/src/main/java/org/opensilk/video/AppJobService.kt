package org.opensilk.video

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.os.PersistableBundle
import dagger.Module
import dagger.Subcomponent
import io.reactivex.Maybe
import io.reactivex.disposables.Disposables
import org.opensilk.common.dagger.Injector
import org.opensilk.common.dagger.injectMe
import org.opensilk.media.*
import org.opensilk.media.database.MediaDAO
import org.opensilk.media.database.UpnpVideoChange
import org.opensilk.media.database.VideoDocumentChange
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


fun Context.scheduleRelatedLookup(mediaId: MediaId) {
    val sched = getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
    val comp = ComponentName(this, AppJobService::class.java)
    val extras = PersistableBundle()
    extras.putString(EXTRA_MEDIAID, mediaId.json)
    val job = JobInfo.Builder(JOB_RELATED_LOOKUP, comp)
            .setExtras(extras)
            .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
            .build()
    sched.schedule(job)
}

/**
 * Created by drew on 7/25/17.
 */
class AppJobService : JobService() {

    @Inject lateinit var mDatabaseClient: MediaDAO
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

    private data class MediaRefWithEpisode(val mediaRef: MediaRef, val episodeRef: TvEpisodeRef)

    fun subscribeUpnpVideoLookupRelated(mediaId: UpnpVideoId, params: JobParameters) {
        relatedLookupSub.dispose()
        relatedLookupSub = mDatabaseClient.playableSiblingsOf(mediaId)
                .flatMapMaybe<MediaRefWithEpisode> { ref ->
                    val title = when (ref) {
                        is UpnpItemRef -> ref.meta.title
                        is DocumentRef -> ref.meta.displayName
                        else -> TODO("unhandled media ref ${ref::javaClass.name}")
                    }
                    val name = extractSeriesName(title)
                    val seasonNum = extractSeasonNumber(title)
                    val episodeNum = extractEpisodeNumber(title)
                    if (name.isNullOrBlank() || seasonNum < 0 || episodeNum < 0) {
                        return@flatMapMaybe Maybe.empty()
                    }
                    return@flatMapMaybe mTvLookup.lookupObservable(LookupRequest(mediaRef = ref,
                            lookupName = name, seasonNumber = seasonNum, episodeNumber = episodeNum))
                            .firstElement()
                            .map { MediaRefWithEpisode(ref, it) }
                }
                .subscribeOn(AppSchedulers.networkIo)
                .subscribe({ vwe ->
                    val ref = vwe.mediaRef
                    val epi = vwe.episodeRef
                    when (ref) {
                        is UpnpVideoRef -> {
                            mDatabaseClient.setUpnpVideoTvEpisodeId(ref.id, epi.id)
                            mDatabaseClient.postChange(UpnpVideoChange(ref.id))
                        }
                        is VideoDocumentRef -> {
                            mDatabaseClient.setVideoDocumentTvEpisodeId(ref.id, epi.id)
                            mDatabaseClient.postChange(VideoDocumentChange(ref.id))
                        }
                        else -> TODO("unhandled media ref ${ref::javaClass.name}")
                    }
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