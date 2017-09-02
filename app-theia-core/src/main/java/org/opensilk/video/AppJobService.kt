package org.opensilk.video

import android.app.DownloadManager
import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.os.Environment
import android.os.PersistableBundle
import android.os.PowerManager
import android.webkit.MimeTypeMap
import dagger.Module
import dagger.android.AndroidInjection
import dagger.android.ContributesAndroidInjector
import io.reactivex.Maybe
import io.reactivex.disposables.Disposable
import org.opensilk.dagger2.ForApp
import org.opensilk.media.*
import org.opensilk.media.database.MediaDAO
import timber.log.Timber
import java.net.SocketTimeoutException
import javax.inject.Inject

private const val JOB_LOOKUP_START = 1000
private const val JOB_LOOKUP_END = 1999
private const val JOB_DOWNLOAD_START = 2000
private const val JOB_DOWNLOAD_END = 2999

private fun Int.isLookupId(): Boolean = this in JOB_LOOKUP_START..JOB_LOOKUP_END
private fun Int.isDownloadId(): Boolean = this in JOB_DOWNLOAD_START..JOB_DOWNLOAD_END

class AppJobScheduler @Inject constructor(
        @ForApp private val mContext: Context
) {
    private val mPrefs = mContext.getSharedPreferences("JobScheduler", Context.MODE_PRIVATE)
    private val mScheduler = mContext.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler

    fun scheduleRelatedLookup(mediaId: MediaId) {
        val extras = PersistableBundle()
        extras.putMediaId(mediaId)
        val id = getNextLookupId()
        val comp = ComponentName(mContext, AppJobService::class.java)
        val job = JobInfo.Builder(id, comp)
                .setExtras(extras)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                //.setOverrideDeadline(TimeUnit.MINUTES.toMillis(1))
                .build()
        mScheduler.schedule(job)
    }

    fun scheduleDownload(mediaId: MediaId) {
        val extras = PersistableBundle()
        extras.putMediaId(mediaId)
        val id = getNextDownloadId()
        val comp = ComponentName(mContext, AppJobService::class.java)
        val job = JobInfo.Builder(id, comp)
                .setExtras(extras)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED)
                //.setOverrideDeadline(TimeUnit.MINUTES.toMillis(1))
                .build()
        mScheduler.schedule(job)
    }

    @Synchronized
    private fun getNextLookupId(): Int {
        var next = mPrefs.getInt("lookup_id", JOB_LOOKUP_START) + 1
        if (next > JOB_LOOKUP_END) {
            next = JOB_LOOKUP_START
        }
        mPrefs.edit().putInt("lookup_id", next).apply()
        return next
    }

    @Synchronized
    private fun getNextDownloadId(): Int {
        var next = mPrefs.getInt("download_id", JOB_DOWNLOAD_START) + 1
        if (next > JOB_DOWNLOAD_END) {
            next = JOB_DOWNLOAD_START
        }
        mPrefs.edit().putInt("download_id", next).apply()
        return next
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
    @Inject lateinit var mFoldersLoader: FoldersLoader

    private lateinit var mDownloadManager: DownloadManager
    private lateinit var mWakelock: PowerManager.WakeLock


    override fun onCreate() {
        AndroidInjection.inject(this)
        super.onCreate()
        mDownloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        mWakelock = (getSystemService(Context.POWER_SERVICE) as PowerManager).newWakeLock(
               PowerManager.PARTIAL_WAKE_LOCK, "JobService")
        mWakelock.acquire()
    }

    override fun onDestroy() {
        super.onDestroy()
        mWakelock.release()
        for (d in jobSubscriptions.values) {
            d.dispose()
        }
        jobSubscriptions.clear()
    }

    private var jobSubscriptions = HashMap<Int, Disposable>()

    override fun onStopJob(params: JobParameters): Boolean {
        Timber.d("onStopJob($params)")
        val id = params.jobId
        return when {
            id.isLookupId() ||
            id.isDownloadId() -> {
                jobSubscriptions.remove(id)?.dispose()
                false
            }
            else -> false
        }
    }

    override fun onStartJob(params: JobParameters): Boolean {
        Timber.d("onStartJob($params)")
        val id = params.jobId
        return when {
            id.isLookupId() -> subscribeLookupRelated(params)
            id.isDownloadId() -> subscribeDownload(params)
            else -> false
        }
    }

    private fun subscribeLookupRelated(params: JobParameters): Boolean {
        val ref = params.extras.getMediaId()
        return when (ref) {
            is VideoId -> {
                subscribeVideoLookupRelated(ref, params)
                true
            }
            else -> false
        }
    }

    private fun subscribeDownload(params: JobParameters): Boolean {
        val ref = params.extras.getMediaId()
        return when (ref) {
            is UpnpVideoId -> {
                subscribeVideoDownload(ref, params)
                true
            }
            is FolderId -> {
                subscribeFolderDownload(ref, params)
                true
            }
            else -> false
        }
    }

    private data class MediaRefWithEpisode(val mediaRef: MediaRef, val episodeRef: TvEpisodeRef)

    private fun subscribeVideoLookupRelated(mediaId: VideoId, params: JobParameters) {
        jobSubscriptions.remove(params.jobId)?.dispose()
        val sub = mDatabaseClient.playableSiblingVideos(mediaId)
                .flatMapMaybe<MediaRefWithEpisode> { ref ->
                    val title = ref.meta.originalTitle.elseIfBlank(ref.meta.title)
                    val name = extractSeriesName(title)
                    val seasonNum = extractSeasonNumber(title)
                    val episodeNum = extractEpisodeNumber(title)
                    if (name.isBlank() || seasonNum < 0 || episodeNum < 0) {
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
                        else -> TODO("unhandled media ref $ref")
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
        jobSubscriptions.put(params.jobId, sub)
    }

    private fun subscribeVideoDownload(videoId: VideoId, params: JobParameters) {
        jobSubscriptions.remove(params.jobId)?.dispose()
        val sub = mDatabaseClient.getVideoRef(videoId).map { meta ->
            val req = DownloadManager.Request(meta.meta.mediaUri)
                    .setDestinationInExternalPublicDir(Environment.DIRECTORY_MOVIES,
                            resolveFilename(meta))
                    .setAllowedOverMetered(false)
                    .setMimeType(meta.meta.mimeType)
                    //.setVisibleInDownloadsUi(false)
                    .setTitle(meta.meta.title)
            req.allowScanningByMediaScanner()
            return@map req
        }.subscribe({ req ->
            mDownloadManager.enqueue(req)
            jobFinished(params, false)
        }, { e ->
            Timber.w(e, "Unsuccessfully enqueued download. $videoId")
            jobFinished(params, false)
        }, {
            jobFinished(params, false)
        })
        jobSubscriptions.put(params.jobId, sub)
    }

    private data class FolderRefWithChildren(val folder: FolderRef, val children: List<MediaRef>)

    private fun subscribeFolderDownload(folderId: FolderId, params: JobParameters) {
        jobSubscriptions.remove(params.jobId)?.dispose()
        val sub = mDatabaseClient.getFolderRef(folderId).flatMapSingle { meta ->
             mFoldersLoader.directChildrenSingle(folderId).map { list ->
                 FolderRefWithChildren(meta, list)
             }
        }.subscribe({ (folder, children) ->
            children.filter { it is VideoRef }.map { it as VideoRef }.forEach { meta ->
                val req = DownloadManager.Request(meta.meta.mediaUri)
                        .setDestinationInExternalPublicDir(Environment.DIRECTORY_MOVIES,
                                  resolveFolderName(folder) + "/" + resolveFilename(meta))
                        .setAllowedOverMetered(false)
                        .setMimeType(meta.meta.mimeType)
                        //.setVisibleInDownloadsUi(false)
                        .setTitle(meta.meta.title)
                req.allowScanningByMediaScanner()
                mDownloadManager.enqueue(req)
            }
            jobFinished(params, false)
        }, { e ->
            Timber.w(e, "Unable to enqueue children of $folderId")
            jobFinished(params, false)
        })
        jobSubscriptions.put(params.jobId, sub)
    }

    private fun resolveFolderName(folderRef: FolderRef): String = folderRef.meta.title

    private fun resolveFilename(videoRef: VideoRef): String {
        val ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(videoRef.meta.mimeType)
        val name = videoRef.meta.originalTitle.elseIfBlank(videoRef.meta.title)
        return if (name.endsWith(ext, true)) name else "$name.$ext"
    }
}
