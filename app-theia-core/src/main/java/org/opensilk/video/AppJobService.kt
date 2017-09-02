package org.opensilk.video

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.os.PersistableBundle
import dagger.Module
import dagger.android.AndroidInjection
import dagger.android.ContributesAndroidInjector
import io.reactivex.Maybe
import io.reactivex.disposables.Disposables
import org.opensilk.dagger2.ForApp
import org.opensilk.media.*
import org.opensilk.media.database.MediaDAO
import timber.log.Timber
import java.net.SocketTimeoutException
import javax.inject.Inject

const val JOB_RELATED_LOOKUP = 1

class AppJobScheduler @Inject constructor(
        @ForApp private val mContext: Context
) {

    fun scheduleRelatedLookup(mediaId: MediaId) {
        val sched = mContext.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
        val comp = ComponentName(mContext, AppJobService::class.java)
        val extras = PersistableBundle()
        extras.putMediaId(mediaId)
        val job = JobInfo.Builder(JOB_RELATED_LOOKUP, comp)
                .setExtras(extras)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .build()
        sched.schedule(job)
    }

}

@Module
abstract class AppJobServiceModule {
    @ContributesAndroidInjector
    abstract fun jobService(): AppJobService
}

/**
 * Created by drew on 7/25/17.
 */
class AppJobService : JobService() {

    @Inject lateinit var mDatabaseClient: MediaDAO
    @Inject lateinit var mTvLookup: LookupTVDb

    override fun onCreate() {
        AndroidInjection.inject(this)
        super.onCreate()
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

    private fun subscribeLookupRelated(params: JobParameters): Boolean {
        val ref = params.extras.getMediaId()
        return when (ref) {
            is UpnpVideoId -> {
                subscribeUpnpVideoLookupRelated(ref, params)
                true
            }
            else -> false
        }
    }

    private data class MediaRefWithEpisode(val mediaRef: MediaRef, val episodeRef: TvEpisodeRef)

    private fun subscribeUpnpVideoLookupRelated(mediaId: UpnpVideoId, params: JobParameters) {
        relatedLookupSub.dispose()
        relatedLookupSub = mDatabaseClient.playableSiblingMedias(mediaId)
                .flatMapMaybe<MediaRefWithEpisode> { ref ->
                    val title = when (ref) {
                        is VideoRef -> ref.meta.originalTitle.elseIfBlank(ref.meta.title)
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
                        is VideoRef -> {
                            mDatabaseClient.setVideoTvEpisodeId(ref.id, epi.id)
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
