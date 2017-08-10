package org.opensilk.video

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.PersistableBundle
import dagger.Binds
import dagger.Module
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.functions.Consumer
import io.reactivex.subjects.BehaviorSubject
import org.opensilk.common.dagger.ForApplication
import org.opensilk.common.rx.cancellationSignal
import org.opensilk.common.rx.subscribeIgnoreError
import org.opensilk.media.*
import org.opensilk.tmdb.api.model.TvEpisode
import org.opensilk.tvdb.api.model.Token
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Module
abstract class MediaProviderModule {
    @Binds
    abstract fun providerClient(providerClient: DatabaseClient): MediaProviderClient
}

/**
 *
 */
class NoSuchItemException: Exception()
class VideoDatabaseMalfuction: Exception()

sealed class DatabaseChange
class UpnpUpdateIdChange(val updateId: Long): DatabaseChange()
class UpnpDeviceChange: DatabaseChange()
class UpnpFolderChange(val folderId: UpnpFolderId): DatabaseChange()
class UpnpVideoChange(val videoId: UpnpVideoId): DatabaseChange()
class DocumentChange(val documentId: DocumentId): DatabaseChange()

/**
 * Created by drew on 7/18/17.
 */
@Singleton
class DatabaseClient
@Inject constructor(
        @ForApplication private val mContext: Context,
        private val mUris: DatabaseUris,
        @Named("tvdb_banner_root") private val mTVDbBannerRoot: String,
        private val mResolver: ContentResolver
) : MediaProviderClient {

    val uris = mUris

    private val mChangesSubject = BehaviorSubject.create<DatabaseChange>()

    val changesObservable: Observable<DatabaseChange>
        get() = mChangesSubject.hide()

    fun upnpVideoChanges(videoId: UpnpVideoId): Observable<UpnpVideoChange> {
        return changesObservable.filter { it is UpnpVideoChange && it.videoId == videoId }
                .map { it as UpnpVideoChange }
    }

    fun postChange(event: DatabaseChange) {
        mChangesSubject.onNext(event)
    }

    override fun getMediaMeta(mediaId: MediaId): Maybe<out MediaRef> {
        return when (mediaId) {
            is UpnpFolderId -> getUpnpFolder(mediaId)
            is UpnpVideoId -> getUpnpVideo(mediaId)
            is UpnpDeviceId -> getUpnpDevice(mediaId)
            is DocumentId -> getDocument(mediaId)
            else -> TODO()
        }
    }


    override fun getMediaArtworkUri(mediaId: MediaId): Maybe<Uri> {
        TODO("not implemented")
    }

    /**
     * This should really be named playableSiblingsOf
     *
     * Do not return folders here
     */
    override fun siblingsOf(mediaId: MediaId): Observable<out MediaRef> {
        return when (mediaId) {
            is UpnpFolderId -> {
                getUpnpFolder(mediaId).flatMapObservable {
                    getUpnpVideosUnder(it.parentId)
                }
            }
            is UpnpVideoId -> {
                getUpnpVideo(mediaId).flatMapObservable {
                    getUpnpVideosUnder(it.parentId)
                }
            }
            is DocumentId -> {
                getDocumentsUnder(mediaId.copy(documentId = mediaId.parentId))
                        .filter { it.isVideo }
            }
            else -> TODO()
        }
    }

    override fun getLastPlaybackPosition(mediaId: MediaId): Maybe<Long> {
        return when (mediaId) {
            is UpnpVideoId -> {
                getUpnpVideo(mediaId).flatMap { meta ->
                    lastPlaybackPosition(meta.meta.mediaTitle)
                }
            }
            is DocumentId -> {
                getDocument(mediaId).flatMap { meta ->
                    lastPlaybackPosition(meta.meta.displayName)
                }
            }
            else -> TODO()
        }
    }

    private fun lastPlaybackPosition(mediaTitle: String): Maybe<Long> {
        return Maybe.create<Long> { s ->
            mResolver.query(mUris.playbackPosition(), arrayOf("last_position"),
                    "_display_name=?", arrayOf(mediaTitle), null, null)?.use { c ->
                if (c.moveToFirst()) {
                    s.onSuccess(c.getLong(0))
                } else {
                    s.onComplete()
                }
            } ?: s.onError(VideoDatabaseMalfuction())
        }
    }

    fun getLastPlaybackCompletion(mediaId: MediaId): Maybe<Int> {
        when (mediaId) {
            is UpnpVideoId -> {
                return getUpnpVideo(mediaId).flatMap { meta ->
                    lastPlaybackCompletion(meta.meta.mediaTitle)
                }
            }
            is DocumentId -> {
                return getDocument(mediaId).flatMap { meta ->
                    lastPlaybackCompletion(meta.meta.displayName)
                }
            }
            else -> TODO()
        }
    }

    private fun lastPlaybackCompletion(mediaTitle: String): Maybe<Int> {
        return Maybe.create<Int> { s ->
            mResolver.query(mUris.playbackPosition(), arrayOf("last_completion"),
                    "_display_name=?", arrayOf(mediaTitle), null, null)?.use { c ->
                if (c.moveToFirst()) {
                    s.onSuccess(c.getInt(0))
                } else {
                    s.onComplete()
                }
            } ?: s.onError(VideoDatabaseMalfuction())
        }
    }

    override fun setLastPlaybackPosition(mediaId: MediaId, position: Long, duration: Long) {
        when (mediaId) {
            is UpnpVideoId -> {
                getUpnpVideo(mediaId)
                        .subscribeOn(AppSchedulers.diskIo)
                        .subscribeIgnoreError(Consumer { meta ->
                            mResolver.insert(mUris.playbackPosition(),
                                    positionContentVals(meta.meta.mediaTitle, position, duration))
                            postChange(UpnpVideoChange(mediaId))
                        })
            }
            is DocumentId -> {
                getDocument(mediaId)
                        .subscribeOn(AppSchedulers.diskIo)
                        .subscribeIgnoreError(Consumer { meta ->
                            mResolver.insert(mUris.playbackPosition(),
                                    positionContentVals(meta.meta.displayName, position, duration))
                            postChange(DocumentChange(mediaId))
                        })
            }
            else -> TODO()
        }
    }

    private fun positionContentVals(mediaTitle: String, position: Long, duration: Long): ContentValues {
        val values = ContentValues()
        values.put("_display_name", mediaTitle)
        values.put("last_played", System.currentTimeMillis())
        values.put("last_position", position)
        values.put("last_completion", calculateCompletion(position, duration))
        return values
    }


    fun getMediaOverview(mediaId: MediaId): Maybe<String> {
        return when (mediaId) {
            is UpnpVideoId -> getUpnpVideoOverview(mediaId)
            is DocumentId -> getDocumentOverview(mediaId)
            else -> TODO()
        }
    }

    /**
     * Add a meta item describing a upnp device with a content directory service to the database
     * item should be created with Device.toMediaMeta
     */
    fun addUpnpDevice(meta: UpnpDeviceRef): Uri {
        val cv = ContentValues()
        cv.put("device_id", meta.id.deviceId)
        cv.put("title", meta.meta.title)
        cv.put("subtitle", meta.meta.subtitle)
        cv.put("artwork_uri", if (meta.meta.artworkUri.isEmpty()) "" else meta.meta.artworkUri.toString())
        cv.put("available", 1)
        cv.put("scanning", 0) //reset here
        return mResolver.insert(mUris.upnpDevices(), cv) ?: Uri.EMPTY
    }

    /**
     * marks the upnp device with giving identity as unavailable
     */
    fun hideUpnpDevice(identity: String): Boolean {
        val cv = ContentValues()
        cv.put("available", 0)
        return mResolver.update(mUris.upnpDevices(), cv, "device_id=?", arrayOf(identity)) != 0
    }

    fun hideAllUpnpDevices() {
        val cv = ContentValues()
        cv.put("available", 0)
        mResolver.update(mUris.upnpDevices(), cv, null, null)
    }

    /**
     * retrieves all the upnp devices marked as available
     */
    fun getUpnpDevices(): Observable<UpnpDeviceRef> {
        return Observable.create { s ->
            mResolver.query(mUris.upnpDevices(), upnpDeviceProjection,
                    "available=1", null, "title", s.cancellationSignal())?.use { c ->
                while (c.moveToNext()) {
                    s.onNext(c.toUpnpDeviceMediaMeta())
                }
                s.onComplete()
            } ?: s.onError(VideoDatabaseMalfuction())
        }
    }

    fun getUpnpDevice(deviceId: UpnpDeviceId): Maybe<UpnpDeviceRef> {
        return Maybe.create { s ->
            mResolver.query(mUris.upnpDevices(), upnpDeviceProjection,
                    "device_id=?", arrayOf(deviceId.deviceId), null, null)?.use { c ->
                if (c.moveToFirst()) {
                    s.onSuccess(c.toUpnpDeviceMediaMeta())
                } else {
                    s.onComplete()
                }
            } ?: s.onError(VideoDatabaseMalfuction())
        }
    }

    fun setUpnpDeviceSystemUpdateId(deviceId: UpnpDeviceId, updateId: Long): Boolean {
        val values = ContentValues()
        values.put("update_id", updateId)
        return mResolver.update(mUris.upnpDevices(), values,
                "device_id=?", arrayOf(deviceId.deviceId)) != 0
    }

    fun getUpnpDeviceSystemUpdateId(deviceId: UpnpDeviceId): Maybe<Long> {
        return Maybe.create { s ->
            mResolver.query(mUris.upnpDevices(), arrayOf("update_id"), "device_id=?",
                    arrayOf(deviceId.deviceId), null, null)?.use { c ->
                if (c.moveToFirst()) {
                    s.onSuccess(c.getLong(0))
                } else {
                    s.onComplete()
                }
            } ?: s.onError(VideoDatabaseMalfuction())
        }
    }

    fun incrementUpnpDeviceScanning(deviceId: UpnpDeviceId): Boolean {
        return mResolver.update(mUris.upnpDeviceIncrementScanning(),
                ContentValues(), "device_id=?", arrayOf(deviceId.deviceId)) != 0
    }

    fun decrementUpnpDeviceScanning(deviceId: UpnpDeviceId): Boolean {
        return mResolver.update(mUris.upnpDeviceDecrementScanning(),
                ContentValues(), "device_id=?", arrayOf(deviceId.deviceId)) != 0
    }

    fun getUpnpDeviceScanning(deviceId: UpnpDeviceId): Maybe<Long> {
        return Maybe.create { s ->
            mResolver.query(mUris.upnpDevices(), arrayOf("scanning"),
                    "device_id=?", arrayOf(deviceId.deviceId), null, null)?.use { c ->
                if (c.moveToFirst()) {
                    s.onSuccess(c.getLong(0))
                } else {
                    s.onComplete()
                }
            } ?: s.onError(VideoDatabaseMalfuction())
        }
    }

    val upnpDeviceProjection = arrayOf("_id", "device_id", "title", //2
            "subtitle", "artwork_uri", "update_id") //5

    fun Cursor.toUpnpDeviceMediaMeta(): UpnpDeviceRef {
        var mediaId = UpnpDeviceId(getString(1))
        var title = getString(2)
        var subtitle = getString(3) ?: ""
        var artworkUri = if (!isNull(4)) Uri.parse(getString(4)) else Uri.EMPTY
        var updateId = getLong(5)
        return UpnpDeviceRef(
                mediaId,
                UpnpDeviceMeta(
                        title = title,
                        subtitle = subtitle,
                        artworkUri = artworkUri,
                        updateId = updateId
                )
        )
    }

    /**
     * Marks hidden column on upnp folders and videos with specified parent
     */
    fun hideChildrenOf(parentId: UpnpFolderId) {
        val cv = ContentValues()
        cv.put("hidden", "1")
        mResolver.update(mUris.upnpFolders(), cv, "device_id=? AND parent_id=?",
                arrayOf(parentId.deviceId, parentId.folderId))
        mResolver.update(mUris.upnpVideos(), cv, "device_id=? AND parent_id=?",
                arrayOf(parentId.deviceId, parentId.folderId))
    }

    /**
     * add a upnp folder to the database, item should be created with Container.toMediaMeta
     */
    fun addUpnpFolder(meta: UpnpFolderRef): Uri {
        val cv = ContentValues()
        cv.put("device_id", meta.id.deviceId)
        cv.put("folder_id", meta.id.folderId)
        cv.put("parent_id", meta.parentId.folderId)
        cv.put("_display_name", meta.meta.title)
        cv.put("artwork_uri", if (meta.meta.artworkUri.isEmpty()) "" else meta.meta.artworkUri.toString())
        cv.put("date_added", System.currentTimeMillis())
        cv.put("hidden", 0)
        return mResolver.insert(uris.upnpFolders(), cv) ?: Uri.EMPTY
    }

    /**
     * retrieve direct decedents of parent folder that aren't hidden
     */
    fun getUpnpFoldersUnder(parentId: UpnpFolderId): Observable<UpnpFolderRef> {
        return Observable.create { s ->
            mResolver.query(mUris.upnpFolders(), upnpFolderProjection,
                    "device_id=? AND parent_id=? AND hidden=0",
                    arrayOf(parentId.deviceId, parentId.folderId),
                    "_display_name", s.cancellationSignal())?.use { c ->
                while (c.moveToNext()) {
                    s.onNext(c.toUpnpFolderMediaMeta())
                }
                s.onComplete()
            } ?: s.onError(VideoDatabaseMalfuction())
        }
    }

    fun getUpnpFolder(folderId: UpnpFolderId): Maybe<UpnpFolderRef> {
        return Maybe.create { s ->
            mResolver.query(uris.upnpFolders(), upnpFolderProjection,
                    "device_id=? AND folder_id=?",
                    arrayOf(folderId.deviceId, folderId.folderId), null, null)?.use { c ->
                if (c.moveToFirst()) {
                    s.onSuccess(c.toUpnpFolderMediaMeta())
                } else {
                    s.onComplete()
                }
            } ?: s.onError(VideoDatabaseMalfuction())
        }
    }

    val upnpFolderProjection = arrayOf("_id", "device_id", "folder_id",
            "parent_id", "_display_name", "artwork_uri")

    fun Cursor.toUpnpFolderMediaMeta(): UpnpFolderRef {
        var mediaId = UpnpFolderId(getString(1), getString(2))
        val parentId = UpnpFolderId(getString(1), getString(3))
        var title = getString(4)
        var arworkUri = if (!isNull(5)) Uri.parse(getString(5)) else Uri.EMPTY
        return UpnpFolderRef(
                mediaId,
                parentId,
                UpnpFolderMeta(
                        title = title,
                        artworkUri = arworkUri
                )
        )
    }

    /**
     * Adds upnp video to database, item should be created with VideoItem.toMediaMeta
     */
    fun addUpnpVideo(meta: UpnpVideoRef): Uri {
        val cv = ContentValues()
        cv.put("device_id", meta.id.deviceId)
        cv.put("item_id", meta.id.itemId)
        cv.put("parent_id", meta.parentId.folderId)
        cv.put("_display_name", meta.meta.mediaTitle)
        cv.put("mime_type", meta.meta.mimeType)
        cv.put("media_uri", meta.meta.mediaUri.toString())
        cv.put("duration", meta.meta.duration)
        cv.put("bitrate", meta.meta.bitrate)
        cv.put("file_size", meta.meta.size)
        cv.put("resolution", meta.meta.resolution)
        cv.put("date_added", System.currentTimeMillis())
        cv.put("hidden", 0)
        return mResolver.insert(uris.upnpVideos(), cv) ?: Uri.EMPTY
    }

    /**
     * retrieve upnp videos, direct decedents of parent
     */
    fun getUpnpVideosUnder(parentId: UpnpFolderId): Observable<UpnpVideoRef> {
        return Observable.create { s ->
            mResolver.query(mUris.upnpVideos(), upnpVideoProjection,
                    "device_id=? AND parent_id=? AND hidden=0", arrayOf(parentId.deviceId, parentId.folderId),
                    "v._display_name", s.cancellationSignal())?.use { c ->
                while (c.moveToNext()) {
                    s.onNext(c.toUpnpVideoMediaMeta())
                }
                s.onComplete()
            } ?: s.onError(VideoDatabaseMalfuction())
        }
    }

    /**
     * retrieve specified upnp video
     */
    fun getUpnpVideo(videoId: UpnpVideoId): Maybe<UpnpVideoRef> {
        return Maybe.create { s ->
            mResolver.query(uris.upnpVideos(), upnpVideoProjection,
                    "device_id=? AND item_id=?",
                    arrayOf(videoId.deviceId, videoId.itemId), null, null)?.use { c ->
                if (c.moveToFirst()) {
                    s.onSuccess(c.toUpnpVideoMediaMeta())
                } else {
                    s.onComplete()
                }
            } ?: s.onError(VideoDatabaseMalfuction())
        }
    }

    fun getRecentUpnpVideos(): Observable<UpnpVideoRef> {
        return Observable.create { s ->
            mResolver.query(mUris.upnpVideos(), upnpVideoProjection, null, null,
                    " v.date_added DESC LIMIT 20 ", s.cancellationSignal())?.use { c ->
                while (c.moveToNext()) {
                    s.onNext(c.toUpnpVideoMediaMeta())
                }
                s.onComplete()
            } ?: s.onError(VideoDatabaseMalfuction())
        }
    }

    /**
     * UpnpVideos with the same mediaTitle prefix
     */
    fun getRelatedUpnpVideos(mediaId: UpnpVideoId): Observable<UpnpVideoRef> {
        return getUpnpVideo(mediaId).flatMapObservable { meta ->
            val name = if (matchesTvEpisode(meta.meta.mediaTitle)) {
                extractSeriesPart(meta.meta.mediaTitle)
            } else ""
            return@flatMapObservable if (name.isBlank()) {
                Observable.error(Exception("Unable to extract series name from mediaTitle"))
            } else {
                Observable.create<UpnpVideoRef> { s ->
                    mResolver.query(mUris.upnpVideos(), upnpVideoProjection,
                            "v._display_name LIKE ?", arrayOf(name+"%"), "v._display_name",
                            s.cancellationSignal())?.use { c ->
                        while (c.moveToNext()) {
                            s.onNext(c.toUpnpVideoMediaMeta())
                        }
                        s.onComplete()
                    } ?: s.onError(VideoDatabaseMalfuction())
                }
            }
        }
    }

    val upnpVideoProjection = arrayOf(
            //upnp_video columns
            "v._id", "device_id", "item_id", "parent_id", //3
            "v._display_name", "mime_type", "media_uri", "duration", "file_size", //8
            //episode columns
            "e._id as episode_id", "e._display_name as episode_title", //10
            "episode_number", "season_number", //12
            //series columns
            "s._id as series_id", "s._display_name as series_title", //14
            "s.poster as series_poster", "s.backdrop as series_backdrop", //16
            //movie columns
            "m._id as movie_id", "m._display_name as movie_title",  //18
            "m.poster_path as movie_poster", //19
            "m.backdrop_path as movie_backdrop", //20
            //more episode columns
            "e.poster as episode_poster", "e.backdrop as episode_backdrop" //22
    )

    /**
     * helper to convert cursor to mediameta using above projection
     */
    fun Cursor.toUpnpVideoMediaMeta(): UpnpVideoRef {
        val c = this
        var mediaId = UpnpVideoId(c.getString(1), c.getString(2))
        var parentId = UpnpFolderId(c.getString(1), c.getString(3))
        var displayName = c.getString(4)
        var title = ""
        var mimeType = c.getString(5)
        var mediaUri = Uri.parse(c.getString(6))
        var duration = c.getLong(7)
        var size = c.getLong(8)
        var subtitle = ""
        var artworkUri = Uri.EMPTY
        var backdropUri = Uri.EMPTY
        var episodeId: TvEpisodeId? = null
        var movieId: MovieId? = null
        if (!c.isNull(9)) {
            episodeId = TvEpisodeId(c.getLong(9), c.getLong(13))
            title = c.getString(10)
            subtitle = makeTvSubtitle(c.getString(14), c.getInt(12), c.getInt(11))
            //prefer episode poster / backdrop over series
            if (!c.isNull(21)) {
                artworkUri = makeTvBannerUri(c.getString(21))
            } else if (!c.isNull(15)) {
                artworkUri = makeTvBannerUri(c.getString(15))
            }
            if (!c.isNull(22)) {
                backdropUri = makeTvBannerUri(c.getString(22))
            } else if (!c.isNull(16)) {
                backdropUri = makeTvBannerUri(c.getString(16))
            }
        } else if (!c.isNull(17)) {
            movieId = MovieId(c.getLong(17))
            title = c.getString(18)
            val movieBaseUrl = getMovieImageBaseUrl()
            if (!c.isNull(19) && movieBaseUrl != "") {
                artworkUri = makeMoviePosterUri(movieBaseUrl, c.getString(19))
            }
            if (!c.isNull(20) && movieBaseUrl != "") {
                backdropUri = makeMovieBackdropUri(movieBaseUrl, c.getString(20))
            }
        }
        return UpnpVideoRef(
                id = mediaId,
                parentId = parentId,
                tvEpisodeId =  episodeId,
                movieId = movieId,
                meta = UpnpVideoMeta(
                        title = title,
                        subtitle = subtitle,
                        artworkUri = artworkUri,
                        backdropUri = backdropUri,
                        mediaTitle = displayName,
                        mediaUri = mediaUri,
                        mimeType = mimeType,
                        duration = duration,
                        size = size
                        //resolution =
                )
        )
    }

    fun scheduleRelatedLookup(mediaId: UpnpVideoId) {
        val sched = mContext.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
        val comp = ComponentName(mContext, AppJobService::class.java)
        val extras = PersistableBundle()
        extras.putString(EXTRA_MEDIAID, mediaId.json)
        val job = JobInfo.Builder(JOB_RELATED_LOOKUP, comp)
                .setExtras(extras)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .build()
        sched.schedule(job)
    }

    /**
     *
     */
    fun getUpnpVideoOverview(videoId: UpnpVideoId): Maybe<String> {
        return Maybe.create { s ->
            mResolver.query(uris.upnpVideos(),
                    arrayOf("e.overview as episode_overview",
                            "m.overview as movie_overview"),
                    "device_id=? AND item_id=?",
                    arrayOf(videoId.deviceId, videoId.itemId),
                    null, null)?.use { c ->
                if (c.moveToFirst()) {
                    var overview = c.getString(0)
                    if (overview.isNullOrBlank()) {
                        overview = c.getString(1)
                    }
                    if (overview.isNullOrBlank()) {
                        s.onComplete()
                    } else {
                        s.onSuccess(overview)
                    }
                } else {
                    s.onComplete()
                }
            } ?: s.onError(VideoDatabaseMalfuction())
        }
    }

    fun setUpnpVideoTvEpisodeId(videoId: UpnpVideoId, episodeId: TvEpisodeId): Boolean {
        val cv = ContentValues()
        cv.put("episode_id", episodeId.episodeId)
        cv.put("movie_id", "")
        return mResolver.update(mUris.upnpVideos(), cv, "device_id=? AND item_id=?",
                arrayOf(videoId.deviceId, videoId.itemId)) != 0
    }

    fun setUpnpVideoMovieId(videoId: UpnpVideoId, movieId: MovieId): Boolean {
        val cv = ContentValues()
        cv.put("episode_id", "")
        cv.put("movie_id", movieId.movieId)
        return mResolver.update(mUris.upnpVideos(), cv, "device_id=? AND item_id=?",
                arrayOf(videoId.deviceId, videoId.itemId)) != 0
    }

    fun addDocument(documentRef: DocumentRef): Uri {
        val values = ContentValues()
        values.put("authority", documentRef.id.treeUri.authority)
        values.put("tree_uri", documentRef.id.treeUri.toString())
        values.put("document_id", documentRef.id.documentId)
        values.put("parent_id", documentRef.id.parentId)
        values.put("_display_name", documentRef.meta.displayName)
        values.put("mime_type", documentRef.meta.mimeType)
        values.put("last_modified", documentRef.meta.lastMod)
        values.put("flags", documentRef.meta.flags)
        values.put("_size", documentRef.meta.size)
        values.put("summary", documentRef.meta.summary)
        values.put("date_added", System.currentTimeMillis())
        values.put("hidden", 0)
        return mResolver.insert(mUris.documents(), values) ?: Uri.EMPTY
    }

    fun getDocumentsUnder(documentId: DocumentId): Observable<DocumentRef> {
        return Observable.create { s ->
            mResolver.query(mUris.documents(), documentProjection,
                    "tree_uri=? AND parent_id=? AND hidden=0",
                    arrayOf(documentId.treeUri.toString(), documentId.documentId),
                    "d._display_name", s.cancellationSignal())?.use { c ->
                while (c.moveToNext()) {
                    s.onNext(c.toDocumentRef())
                }
                s.onComplete()
            } ?: s.onError(VideoDatabaseMalfuction())
        }
    }

    fun getDocument(documentId: DocumentId): Maybe<DocumentRef> {
        return Maybe.create { s ->
            mResolver.query(mUris.documents(), documentProjection,
                    "tree_uri=? AND document_id=? AND parent_id=?",
                    arrayOf(documentId.treeUri.toString(), documentId.documentId,
                            documentId.parentId), null, null)?.use { c ->
                if (c.moveToFirst()) {
                    s.onSuccess(c.toDocumentRef())
                } else {
                    s.onComplete()
                }
            } ?: s.onError(VideoDatabaseMalfuction())
        }
    }

    val documentProjection = arrayOf(
            //document columns
            "d._id", "tree_uri", "document_id", "parent_id", //3
            "d._display_name", "d.mime_type", "d._size", "d.flags", "d.last_modified", //8
            //episode columns
            "e._id as episode_id", "e._display_name as episode_title", //10
            "episode_number", "season_number", //12
            //series columns
            "s._id as series_id", "s._display_name as series_title", //14
            "s.poster as series_poster", "s.backdrop as series_backdrop", //16
            //movie columns
            "m._id as movie_id", "m._display_name as movie_title",  //18
            "m.poster_path as movie_poster", //19
            "m.backdrop_path as movie_backdrop", //20
            //more episode columns
            "e.poster as episode_poster", "e.backdrop as episode_backdrop" //22
    )

    /**
     * helper to convert cursor to mediameta using above projection
     */
    fun Cursor.toDocumentRef(): DocumentRef {
        val treeUri = Uri.parse(getString(1))
        val docId = getString(2)
        val parentId = getString(3)
        val displayName = getString(4)
        var title = ""
        val mimeType = getString(5)
        val size = if (!isNull(6)) getLong(6) else 0L
        val flags = if (!isNull(7)) getLong(7) else 0L
        val lastMod = if (!isNull(8)) getLong(8) else 0L
        var subtitle = ""
        var artworkUri = Uri.EMPTY
        var backdropUri = Uri.EMPTY
        var episodeId: TvEpisodeId? = null
        var movieId: MovieId? = null
        if (!isNull(9)) {
            episodeId = TvEpisodeId(getLong(9), getLong(13))
            title = getString(10)
            subtitle = makeTvSubtitle(getString(14), getInt(12), getInt(11))
            //prefer episode poster / backdrop over series
            if (!isNull(21)) {
                artworkUri = makeTvBannerUri(getString(21))
            } else if (!isNull(15)) {
                artworkUri = makeTvBannerUri(getString(15))
            }
            if (!isNull(22)) {
                backdropUri = makeTvBannerUri(getString(22))
            } else if (!isNull(16)) {
                backdropUri = makeTvBannerUri(getString(16))
            }
        } else if (!isNull(17)) {
            movieId = MovieId(getLong(17))
            title = getString(18)
            val movieBaseUrl = getMovieImageBaseUrl()
            if (!isNull(19) && movieBaseUrl != "") {
                artworkUri = makeMoviePosterUri(movieBaseUrl, getString(19))
            }
            if (!isNull(20) && movieBaseUrl != "") {
                backdropUri = makeMovieBackdropUri(movieBaseUrl, getString(20))
            }
        }
        return DocumentRef(
                id = DocumentId(
                        treeUri = treeUri,
                        documentId = docId,
                        parentId = parentId
                ),
                tvEpisodeId =  episodeId,
                movieId = movieId,
                meta = DocumentMeta(
                        title = title,
                        subtitle = subtitle,
                        artworkUri = artworkUri,
                        backdropUri = backdropUri,
                        displayName = displayName,
                        mimeType = mimeType,
                        size = size,
                        flags = flags,
                        lastMod = lastMod
                        //summary,
                )
        )
    }

    fun hideDocumentsUnder(documentId: DocumentId): Boolean {
        val values = ContentValues()
        values.put("hidden", 1)
        return mResolver.update(mUris.documents(), values, "tree_uri=? AND document_id=? AND parent_id=?",
                arrayOf(documentId.treeUri.toString(), documentId.documentId, documentId.parentId)) != 0
    }

    fun getDocumentOverview(documentId: DocumentId): Maybe<String> {
        return Maybe.create { s ->
            mResolver.query(mUris.documents(),
                    arrayOf("e.overview as episode_overview",
                            "m.overview as movie_overview"),
                    "tree_uri=? AND document_id=? AND parent_id=?",
                    arrayOf(documentId.treeUri.toString(), documentId.documentId,
                            documentId.parentId), null, null)?.use { c ->
                if (c.moveToFirst()) {
                    var overview = c.getString(0)
                    if (overview.isNullOrBlank()) {
                        overview = c.getString(1)
                    }
                    if (overview.isNullOrBlank()) {
                        s.onComplete()
                    } else {
                        s.onSuccess(overview)
                    }
                } else {
                    s.onComplete()
                }
            } ?: s.onError(VideoDatabaseMalfuction())
        }
    }

    fun setDocumentTvEpisodeId(documentId: DocumentId, tvEpisodeId: TvEpisodeId): Boolean {
        val values = ContentValues()
        values.put("episode_id", tvEpisodeId.episodeId)
        values.put("movie_id", "")
        return mResolver.update(mUris.documents(), values, "tree_uri=? AND document_id=? AND parent_id=?",
                arrayOf(documentId.treeUri.toString(), documentId.documentId, documentId.parentId)) != 0
    }

    fun setDocumentMovieId(documentId: DocumentId, movieId: MovieId): Boolean {
        val values = ContentValues()
        values.put("episode_id", "")
        values.put("movie_id", movieId.movieId)
        return mResolver.update(mUris.documents(), values, "tree_uri=? AND document_id=? AND parent_id=?",
                arrayOf(documentId.treeUri.toString(), documentId.documentId, documentId.parentId)) != 0
    }

    fun makeTvBannerUri(path: String): Uri {
        return Uri.parse(mTVDbBannerRoot).buildUpon().appendPath(path).build()
    }

    fun makeTvSubtitle(seriesName: String, seasonNumber: Int, episodeNumber: Int): String {
        return "$seriesName - S${seasonNumber.zeroPad(2)}E${episodeNumber.zeroPad(2)}"
    }

    fun setTvLastUpdate(lastUpdate: Long) {
        val cv = ContentValues()
        cv.put("key", "last_update")
        cv.put("value", lastUpdate)
        mResolver.insert(mUris.tvConfig(), cv)
    }

    fun getTvLastUpdate(): Maybe<Long> {
        return Maybe.create { s ->
            mResolver.query(mUris.tvConfig(), arrayOf("value"),
                    "key='last_update'", null, null, null)?.use { c ->
                if (c.moveToFirst()) {
                    s.onSuccess(c.getString(0).toLong())
                } else {
                    s.onComplete()
                }
            } ?: s.onError(VideoDatabaseMalfuction())
        }
    }

    fun setTvToken(token: Token) {
        val cv = ContentValues()
        cv.put("key", "token")
        cv.put("value", token.token)
        mResolver.insert(mUris.tvConfig(), cv)
    }

    fun getTvToken(): Single<Token> {
        return Single.create { s ->
            mResolver.query(mUris.tvConfig(), arrayOf("value"),
                    "key='token'", null, null, null)?.use { c ->
                if (c.moveToFirst()) {
                    s.onSuccess(Token(c.getString(0)))
                } else {
                    s.onError(Exception("Token not found"))
                }
            } ?: s.onError(VideoDatabaseMalfuction())
        }
    }


    fun addTvSeries(series: TvSeriesRef): Uri {
        val values = ContentValues(10)
        values.put("_id", series.id.seriesId)
        values.put("_display_name", series.meta.title)
        values.put("overview", series.meta.overview)
        values.put("first_aired", series.meta.releaseDate)
        values.put("poster", series.meta.posterPath)
        values.put("backdrop", series.meta.backdropPath)
        return mResolver.insert(mUris.tvSeries(), values) ?: Uri.EMPTY
    }

    fun getTvSeries(seriesId: TvSeriesId): Maybe<TvSeriesRef> {
        return Maybe.create { s ->
            mResolver.query(mUris.tvSeries(seriesId.seriesId),
                    tvSeriesProjection, null, null, null, null)?.use { c ->
                if (c.moveToFirst()) {
                    val meta = TvSeriesRef(
                            TvSeriesId(c.getLong(3)),
                            TvSeriesMeta(
                                    title = c.getString(0),
                                    overview = c.getString(1) ?: "",
                                    releaseDate = c.getString(2),
                                    posterPath = c.getString(4) ?: "",
                                    backdropPath = c.getString(5) ?: ""
                            )
                    )
                    s.onSuccess(meta)
                } else {
                    s.onComplete()
                }
            } ?: s.onError(VideoDatabaseMalfuction())
        }
    }

    val tvSeriesProjection = arrayOf("_display_name", "overview",
            "first_aired", "_id", "poster", "backdrop")

    fun addTvEpisodes(episodes: List<TvEpisodeRef>) {
        val values = Array(episodes.size, { ContentValues() })
        for ((ii, episode) in episodes.withIndex()) {
            values[ii].putTvEpisodeValues(episode)
        }
        mResolver.bulkInsert(mUris.tvEpisodes(), values)
    }


    fun ContentValues.putTvEpisodeValues(episode: TvEpisodeRef) {
        val values = this
        values.put("_id", episode.id.episodeId)
        values.put("series_id", episode.id.seriesId)
        values.put("_display_name", episode.meta.title)
        values.put("overview", episode.meta.overview)
        values.put("first_aired", episode.meta.releaseDate)
        values.put("episode_number", episode.meta.episodeNumber)
        values.put("season_number", episode.meta.seasonNumber)
        values.put("poster", episode.meta.posterPath)
        values.put("backdrop", episode.meta.backdropPath)
    }

    fun getTvEpisodesForTvSeries(seriesId: TvSeriesId): Observable<TvEpisodeRef> {
        return Observable.create { s ->
            mResolver.query(mUris.tvEpisodes(), tvEpisodesProjection,
                    "series_id=${seriesId.seriesId}", null,
                    null, s.cancellationSignal())?.use { c ->
                while (c.moveToNext()) {
                    s.onNext(c.toTvEpisodeMediaMeta())
                }
                s.onComplete()
            } ?: s.onError(VideoDatabaseMalfuction())
        }
    }

    fun getTvEpisode(episodeId: TvEpisodeId): Maybe<TvEpisodeRef> {
        return Maybe.create { s ->
            mResolver.query(mUris.tvEpisode(episodeId.episodeId), tvEpisodesProjection,
                    null, null, null, null)?.use { c ->
                if (c.moveToFirst()) {
                    s.onSuccess(c.toTvEpisodeMediaMeta())
                } else {
                    s.onComplete()
                }
            } ?: s.onError(VideoDatabaseMalfuction())
        }
    }

    val tvEpisodesProjection = arrayOf(
            "_id", "_display_name", "first_aired",
            "episode_number", "season_number",
            "overview", "series_id", "poster", "backdrop")

    fun Cursor.toTvEpisodeMediaMeta(): TvEpisodeRef {
        return TvEpisodeRef(
                TvEpisodeId(getLong(0), getLong(6)),
                TvEpisodeMeta(
                        title = getString(1),
                        releaseDate = getString(2) ?: "",
                        episodeNumber = getInt(3),
                        seasonNumber = getInt(4),
                        overview = getString(5) ?: "",
                        posterPath = getString(7) ?: "",
                        backdropPath = getString(8) ?: ""
                )
        )
    }

    fun addTvImages(banners: List<TvImageRef>) {
        val values = Array(banners.size, { ContentValues() })
        for ((ii, image) in banners.withIndex()) {
            values[ii].putTvImageValues(image)
        }
        mResolver.bulkInsert(mUris.tvBanners(), values)
    }

    fun ContentValues.putTvImageValues(banner: TvImageRef) {
        val values = this
        values.put("_id", banner.id.imageId)
        values.put("series_id", banner.id.seriesId)
        values.put("path", banner.meta.path)
        values.put("type", banner.meta.type)
        values.put("type2", banner.meta.subType)
        values.put("rating", banner.meta.rating)
        values.put("rating_count", banner.meta.ratingCount)
        values.put("resolution", banner.meta.resolution)
    }

    fun getTvPosters(seriesId: TvSeriesId): Observable<TvImageRef> {
        return Observable.create { s ->
            mResolver.query(mUris.tvBanners(), tvBannerProjection,
                    "series_id=? and type=?", arrayOf(seriesId.seriesId.toString(), "poster"),
                    "rating DESC", s.cancellationSignal())?.use { c ->
                while (c.moveToNext()) {
                    s.onNext(c.toTvBannerMediaMeta())
                }
                s.onComplete()
            } ?: s.onError(VideoDatabaseMalfuction())
        }
    }

    fun getTvBackdrops(seriesId: TvSeriesId): Observable<TvImageRef> {
        return Observable.create { s ->
            mResolver.query(mUris.tvBanners(), tvBannerProjection,
                    "series_id=? and type=?", arrayOf(seriesId.seriesId.toString(), "fanart"),
                    "rating DESC", s.cancellationSignal())?.use { c ->
                while (c.moveToNext()) {
                    s.onNext(c.toTvBannerMediaMeta())
                }
                s.onComplete()
            } ?: s.onError(VideoDatabaseMalfuction())
        }
    }

    val tvBannerProjection = arrayOf("_id", "series_id",
            "path", "type", "type2", "rating",
            "rating_count", "resolution")

    fun Cursor.toTvBannerMediaMeta(): TvImageRef {
        return TvImageRef(
                TvImageId(getLong(0),getLong(1)),
                TvImageMeta(
                        path =  getString(2),
                        type = getString(3),
                        subType = getString(4) ?: "",
                        rating = if (!isNull(5)) getFloat(5) else 0f,
                        ratingCount = if (!isNull(6)) getInt(6) else 0,
                        resolution = getString(7) ?: ""
                )
        )
    }

    fun makeMoviePosterUri(base: String, path: String): Uri {
        return Uri.parse("${base}w342$path")
    }

    fun makeMovieBackdropUri(base: String, path: String): Uri {
        return Uri.parse("${base}w1280$path")
    }

    @Synchronized
    fun setMovieImageBaseUrl(imageBaseUrl: String): Boolean {
        val values = ContentValues()
        values.put("key", "image_base_url")
        values.put("value", imageBaseUrl)
        return mResolver.insert(mUris.movieConfig(), values) != null
    }

    @Synchronized
    fun getMovieImageBaseUrl(): String {
        return mResolver.query(mUris.movieConfig(), arrayOf("value"),
                "key='image_base_url'", null, null, null)?.use { c ->
            if (c.moveToFirst()) {
                return@use c.getString(0)
            } else {
                return@use ""
            }
        } ?: throw VideoDatabaseMalfuction()
    }

    fun addMovie(movie: MovieRef): Uri {
        val values = ContentValues(10)
        values.put("_id", movie.id.movieId)
        values.put("_display_name", movie.meta.title)
        values.put("overview", movie.meta.overview)
        values.put("release_date", movie.meta.releaseDate)
        values.put("poster_path", movie.meta.posterPath)
        values.put("backdrop_path", movie.meta.backdropPath)
        return mResolver.insert(mUris.movies(), values) ?: Uri.EMPTY
    }

    fun getMovie(movieId: MovieId): Maybe<MovieRef> {
        return Maybe.create { s ->
            mResolver.query(mUris.movie(movieId.movieId), movieProjection, null,
                    null, null, null )?.use { c ->
                if (c.moveToFirst()) {
                    s.onSuccess(c.toMovieRef())
                } else {
                    s.onComplete()
                }
            } ?: s.onError(VideoDatabaseMalfuction())
        }
    }

    val movieProjection = arrayOf("_display_name", "overview",
            "release_date", "poster_path", "backdrop_path", "_id")

    fun Cursor.toMovieRef(): MovieRef {
        val title = getString(0)
        val overview = getString(1) ?: ""
        val releaseDate = getString(2) ?: ""
        val arwork = getString(3) ?: ""
        val backdrop = getString(4) ?: ""
        val rowId = getLong(5)
        return MovieRef(
                MovieId(rowId),
                MovieMeta(
                        title = title,
                        overview = overview,
                        releaseDate = releaseDate,
                        posterPath = arwork,
                        backdropPath = backdrop
                )
        )
    }

    fun addMovieImages(images: List<MovieImageRef>) {
        val contentValues = Array(images.size, { ContentValues() })
        for ((idx, image) in images.withIndex()) {
            contentValues[idx].makeImageValues(image)
        }
        mResolver.bulkInsert(mUris.movieImages(), contentValues)
    }

    fun ContentValues.makeImageValues(image: MovieImageRef): ContentValues {
        val values = this
        if (image.id.imageId > 0) {
            values.put("_id", image.id.imageId)
        }
        values.put("movie_id", image.id.movieId)
        values.put("image_type", image.meta.type)
        values.put("file_path", image.meta.path)
        values.put("resolution", image.meta.resolution)
        values.put("vote_average", image.meta.rating)
        values.put("vote_count", image.meta.ratingCount)
        return values
    }

}
