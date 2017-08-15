package org.opensilk.media.database

import android.content.ContentResolver
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.OperationCanceledException
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.functions.Consumer
import io.reactivex.subjects.BehaviorSubject
import org.opensilk.media.*
import org.opensilk.reactivex2.cancellationSignal
import org.opensilk.reactivex2.subscribeIgnoreError
import javax.inject.Inject
import javax.inject.Singleton

interface ApiHelper {
    fun tvImagePosterUri(path: String): Uri
    fun tvImageBackdropUri(path: String): Uri
    fun movieImagePosterUri(path: String): Uri
    fun movieImageBackdropUri(path: String): Uri
}

/**
 * Created by drew on 7/18/17.
 */
@Singleton
class MediaDAO
@Inject constructor(
        private val mResolver: ContentResolver,
        private val mUris: MediaDBUris,
        private val mApiHelper: ApiHelper
) {

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

    private fun <T> doQuery(uri: Uri, projection: Array<out String>?, selection: String?,
                    selectionArgs: Array<out String>?, sortOrder: String?,
                    converter: (c: Cursor) -> T): Observable<T> {
        return Observable.create { s ->
            try {
                mResolver.query(uri, projection, selection, selectionArgs,
                        sortOrder, s.cancellationSignal())?.use { c ->
                    while (c.moveToNext()) {
                        s.onNext(converter(c))
                    }
                    s.onComplete()
                } ?: s.onError(VideoDatabaseMalfuction())
            } catch (e: OperationCanceledException) {
                //pass
            }
        }
    }

    fun getMediaRef(mediaId: MediaId): Maybe<out MediaRef> = when (mediaId) {
        is UpnpFolderId -> getUpnpFolder(mediaId)
        is UpnpVideoId -> getUpnpVideo(mediaId)
        is UpnpDeviceId -> getUpnpDevice(mediaId)
        is DocumentId -> when {
            mediaId.isDirectory -> getDirectoryDocument(mediaId)
            mediaId.isVideo -> getVideoDocument(mediaId)
            else -> TODO()
        }
        else -> TODO()
    }

    fun getMediaOverview(mediaId: MediaId): Maybe<String> = when (mediaId) {
        is UpnpVideoId -> getUpnpVideoOverview(mediaId)
        is DocumentId -> when {
            mediaId.isVideo -> getVideoDocumentOverview(mediaId)
            else -> TODO()
        }
        else -> TODO()
    }

    /**
     * Do not return folders here
     */
    fun playableSiblingsOf(mediaId: MediaId): Observable<out MediaRef> = when (mediaId) {
        is UpnpFolderId -> {
            getUpnpVideosUnder(UpnpFolderId(mediaId.deviceId, "", mediaId.parentId))
        }
        is UpnpVideoId -> {
            getUpnpVideosUnder(UpnpFolderId(mediaId.deviceId, "", mediaId.parentId))
        }
        is DocumentId -> {
            if (mediaId.isVideo) {
                getVideoDocumentsUnder(mediaId.copy(documentId = mediaId.parentId))
            } else TODO()
        }
        else -> TODO()
    }

    fun getLastPlaybackPosition(mediaId: MediaId): Maybe<Long> = when (mediaId) {
        is UpnpVideoId -> {
            getUpnpVideo(mediaId).flatMap { meta ->
                lastPlaybackPosition(meta.meta.originalTitle)
            }
        }
        is DocumentId -> when {
            mediaId.isVideo -> {
                getVideoDocument(mediaId).flatMap { meta ->
                    lastPlaybackPosition(meta.meta.displayName)
                }
            }
            else -> TODO()
        }
        else -> TODO()
    }



    fun lastPlaybackPosition(mediaTitle: String): Maybe<Long> {
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

    fun getLastPlaybackCompletion(mediaId: MediaId): Maybe<Int> = when (mediaId) {
        is UpnpVideoId -> {
            getUpnpVideo(mediaId).flatMap { meta ->
                lastPlaybackCompletion(meta.meta.originalTitle)
            }
        }
        is DocumentId -> when {
            mediaId.isVideo -> {
                getVideoDocument(mediaId).flatMap { meta ->
                    lastPlaybackCompletion(meta.meta.displayName)
                }
            }
            else -> TODO()
        }
        else -> TODO()
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

    fun setLastPlaybackPosition(mediaId: MediaId, position: Long, duration: Long) {
        when (mediaId) {
            is UpnpVideoId -> {
                getUpnpVideo(mediaId)
                        .subscribeIgnoreError(Consumer { meta ->
                            mResolver.insert(mUris.playbackPosition(),
                                    positionContentVals(meta.meta.originalTitle, position, duration))
                            postChange(UpnpVideoChange(mediaId))
                        })
            }
            is DocumentId -> {
                if (mediaId.isVideo) {
                    getVideoDocument(mediaId)
                            .subscribeIgnoreError(Consumer { meta ->
                                mResolver.insert(mUris.playbackPosition(),
                                        positionContentVals(meta.meta.displayName, position, duration))
                                postChange(VideoDocumentChange(mediaId))
                            })
                } else TODO()
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

    /**
     * Add a meta item describing a upnp device with a content directory service to the database
     * item should be created with Device.toMediaMeta
     */
    fun addUpnpDevice(upnpDevice: UpnpDeviceRef): Boolean {
        return mResolver.insert(mUris.upnpDevice(), upnpDevice.contentValues()) == URI_SUCCESS
    }

    /**
     * marks the upnp device with giving identity as unavailable
     */
    fun hideUpnpDevice(identity: String): Boolean {
        val cv = ContentValues()
        cv.put("available", 0)
        return mResolver.update(mUris.upnpDevice(), cv, "device_id=?", arrayOf(identity)) != 0
    }

    fun hideAllUpnpDevices() {
        val cv = ContentValues()
        cv.put("available", 0)
        mResolver.update(mUris.upnpDevice(), cv, null, null)
    }



    /**
     * retrieves all the upnp devices marked as available
     */
    fun getUpnpDevices(): Observable<UpnpDeviceRef> {
        return doQuery(mUris.upnpDevice(), upnpDeviceProjection,
                "available=1", null, "title", {c -> c.toUpnpDeviceRef()})
    }

    fun getUpnpDevice(deviceId: UpnpDeviceId): Maybe<UpnpDeviceRef> {
        return Maybe.create { s ->
            mResolver.query(mUris.upnpDevice(), upnpDeviceProjection,
                    "device_id=?", arrayOf(deviceId.deviceId), null, null)?.use { c ->
                if (c.moveToFirst()) {
                    s.onSuccess(c.toUpnpDeviceRef())
                } else {
                    s.onComplete()
                }
            } ?: s.onError(VideoDatabaseMalfuction())
        }
    }

    fun setUpnpDeviceSystemUpdateId(deviceId: UpnpDeviceId, updateId: Long): Boolean {
        val values = ContentValues()
        values.put("update_id", updateId)
        return mResolver.update(mUris.upnpDevice(), values,
                "device_id=?", arrayOf(deviceId.deviceId)) != 0
    }

    fun getUpnpDeviceSystemUpdateId(deviceId: UpnpDeviceId): Maybe<Long> {
        return Maybe.create { s ->
            mResolver.query(mUris.upnpDevice(), arrayOf("update_id"), "device_id=?",
                    arrayOf(deviceId.deviceId), null, null)?.use { c ->
                if (c.moveToFirst()) {
                    s.onSuccess(c.getLong(0))
                } else {
                    s.onComplete()
                }
            } ?: s.onError(VideoDatabaseMalfuction())
        }
    }

    /**
     * add a upnp folder to the database, item should be created with Container.toMediaMeta
     */
    fun addUpnpFolder(folderRef: UpnpFolderRef): Boolean {
        return mResolver.insert(mUris.upnpFolder(), folderRef.contentValues()) == URI_SUCCESS
    }

    /**
     * retrieve direct decedents of parent folder that aren't hidden
     */
    fun getUpnpFoldersUnder(parentId: UpnpContainerId): Observable<UpnpFolderRef> {
        return doQuery(mUris.upnpFolder(), upnpFolderProjection,
                "device_id=? AND parent_id=? AND hidden=0",
                arrayOf(parentId.deviceId, parentId.containerId),
                "_display_name", { c -> c.toUpnpFolderRef() })
    }

    fun getUpnpFolder(folderId: UpnpFolderId): Maybe<UpnpFolderRef> {
        return Maybe.create { s ->
            mResolver.query(mUris.upnpFolder(), upnpFolderProjection,
                    "device_id=? AND parent_id=? AND folder_id=?",
                    arrayOf(folderId.deviceId, folderId.parentId, folderId.containerId), null, null)?.use { c ->
                if (c.moveToFirst()) {
                    s.onSuccess(c.toUpnpFolderRef())
                } else {
                    s.onComplete()
                }
            } ?: s.onError(VideoDatabaseMalfuction())
        }
    }

    /**
     * Adds upnp video to database, item should be created with VideoItem.toMediaMeta
     */
    fun addUpnpVideo(videoRef: UpnpVideoRef): Boolean {
        return mResolver.insert(mUris.upnpVideo(), videoRef.contentValues()) == URI_SUCCESS
    }

    /**
     * retrieve upnp videos, direct decedents of parent
     */
    fun getUpnpVideosUnder(parentId: UpnpContainerId): Observable<UpnpVideoRef> {
        return doQuery(mUris.upnpVideo(), upnpVideoProjection,
                "device_id=? AND parent_id=? AND hidden=0",
                arrayOf(parentId.deviceId, parentId.containerId),
                "v._display_name", { c ->c.toUpnpVideoMediaMeta(mApiHelper) })
    }

    /**
     * retrieve specified upnp video
     */
    fun getUpnpVideo(videoId: UpnpVideoId): Maybe<UpnpVideoRef> {
        return Maybe.create { s ->
            mResolver.query(mUris.upnpVideo(), upnpVideoProjection,
                    "device_id=? AND parent_id=? AND item_id=?",
                    arrayOf(videoId.deviceId, videoId.parentId, videoId.itemId), null, null)?.use { c ->
                if (c.moveToFirst()) {
                    s.onSuccess(c.toUpnpVideoMediaMeta(mApiHelper))
                } else {
                    s.onComplete()
                }
            } ?: s.onError(VideoDatabaseMalfuction())
        }
    }

    fun getRecentUpnpVideos(): Observable<UpnpVideoRef> {
        return doQuery(mUris.upnpVideo(), upnpVideoProjection, null, null,
                " v.date_added DESC LIMIT 20 ", { c -> c.toUpnpVideoMediaMeta(mApiHelper) })
    }

    fun getUpnpVideoOverview(videoId: UpnpVideoId): Maybe<String> {
        return Maybe.create { s ->
            mResolver.query(mUris.upnpVideo(),
                    arrayOf("e.overview as episode_overview",
                            "m.overview as movie_overview"),
                    "device_id=? AND parent_id=? AND item_id=?",
                    arrayOf(videoId.deviceId, videoId.parentId, videoId.itemId),
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
        return mResolver.update(mUris.upnpVideo(), cv,
                "device_id=? AND parent_id=? AND item_id=?",
                arrayOf(videoId.deviceId, videoId.parentId, videoId.itemId)) != 0
    }

    fun setUpnpVideoMovieId(videoId: UpnpVideoId, movieId: MovieId): Boolean {
        val cv = ContentValues()
        cv.put("episode_id", "")
        cv.put("movie_id", movieId.movieId)
        return mResolver.update(mUris.upnpVideo(), cv,
                "device_id=? AND parent_id=? AND item_id=?",
                arrayOf(videoId.deviceId, videoId.parentId, videoId.itemId)) != 0
    }

    /**
     * Marks hidden column on upnp folders and videos with specified parent
     */
    fun hideChildrenOf(parentId: UpnpContainerId) {
        val cv = ContentValues()
        cv.put("hidden", "1")
        mResolver.update(mUris.upnpFolder(), cv, "device_id=? AND parent_id=?",
                arrayOf(parentId.deviceId, parentId.containerId))
        mResolver.update(mUris.upnpVideo(), cv, "device_id=? AND parent_id=?",
                arrayOf(parentId.deviceId, parentId.containerId))
    }

    /*
     * END UPNP
     */

    /*
     * START DOCUMENT
     */

    fun addDirectoryDocument(documentRef: DirectoryDocumentRef): Boolean {
        return mResolver.insert(mUris.documentDirectory(), documentRef.contentValues()) == URI_SUCCESS
    }

    fun getDirectoryDocumentsUnder(documentId: DocumentId): Observable<DirectoryDocumentRef> {
        return doQuery(mUris.documentDirectory(), directoryDocumentProjection,
                "tree_uri=? AND parent_id=? AND hidden=0",
                arrayOf(documentId.treeUri.toString(), documentId.documentId),
                "_display_name", { c ->c.toDirectoryDocument() })
    }

    fun getDirectoryDocument(documentId: DocumentId): Maybe<DirectoryDocumentRef> {
        return Maybe.create { s ->
            mResolver.query(mUris.documentDirectory(), directoryDocumentProjection,
                    "tree_uri=? AND document_id=? AND parent_id=?",
                    arrayOf(documentId.treeUri.toString(), documentId.documentId,
                            documentId.parentId), null, null)?.use { c ->
                if (c.moveToFirst()) {
                    s.onSuccess(c.toDirectoryDocument())
                } else {
                    s.onComplete()
                }
            } ?: s.onError(VideoDatabaseMalfuction())
        }
    }

    fun addVideoDocument(documentRef: VideoDocumentRef): Boolean {
        return mResolver.insert(mUris.documentVideo(), documentRef.contentValues()) == URI_SUCCESS
    }

    fun getVideoDocumentsUnder(documentId: DocumentId): Observable<VideoDocumentRef> {
        return doQuery(mUris.documentVideo(), videoDocumentProjection,
                "tree_uri=? AND parent_id=? AND hidden=0",
                arrayOf(documentId.treeUri.toString(), documentId.documentId),
                "d._display_name", { c -> c.toVideoDocumentRef(mApiHelper) })
    }

    fun getVideoDocument(documentId: DocumentId): Maybe<VideoDocumentRef> {
        return Maybe.create { s ->
            mResolver.query(mUris.documentVideo(), videoDocumentProjection,
                    "tree_uri=? AND document_id=? AND parent_id=?",
                    arrayOf(documentId.treeUri.toString(), documentId.documentId,
                            documentId.parentId), null, null)?.use { c ->
                if (c.moveToFirst()) {
                    s.onSuccess(c.toVideoDocumentRef(mApiHelper))
                } else {
                    s.onComplete()
                }
            } ?: s.onError(VideoDatabaseMalfuction())
        }
    }

    fun getRecentVideoDocuments(): Observable<VideoDocumentRef> {
        return doQuery(mUris.documentVideo(), videoDocumentProjection, null, null,
                "d.date_added DESC LIMIT 20", { c -> c.toVideoDocumentRef(mApiHelper)) })
    }

    fun getVideoDocumentOverview(documentId: DocumentId): Maybe<String> {
        return Maybe.create { s ->
            mResolver.query(mUris.documentVideo(),
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

    fun setVideoDocumentTvEpisodeId(documentId: DocumentId, tvEpisodeId: TvEpisodeId): Boolean {
        val values = ContentValues()
        values.put("episode_id", tvEpisodeId.episodeId)
        values.put("movie_id", "")
        return mResolver.update(mUris.documentVideo(), values, "tree_uri=? AND document_id=? AND parent_id=?",
                arrayOf(documentId.treeUri.toString(), documentId.documentId, documentId.parentId)) != 0
    }

    fun setVideoDocumentMovieId(documentId: DocumentId, movieId: MovieId): Boolean {
        val values = ContentValues()
        values.put("episode_id", "")
        values.put("movie_id", movieId.movieId)
        return mResolver.update(mUris.documentVideo(), values, "tree_uri=? AND document_id=? AND parent_id=?",
                arrayOf(documentId.treeUri.toString(), documentId.documentId, documentId.parentId)) != 0
    }

    fun hideChildrenOf(documentId: DocumentId): Boolean {
        val values = ContentValues()
        values.put("hidden", 1)
        var num = 0
        num += mResolver.update(mUris.documentDirectory(), values,
                "tree_uri=? AND document_id=? AND parent_id=?",
                arrayOf(documentId.treeUri.toString(), documentId.documentId, documentId.parentId))
        num += mResolver.update(mUris.documentVideo(), values,
                "tree_uri=? AND document_id=? AND parent_id=?",
                arrayOf(documentId.treeUri.toString(), documentId.documentId, documentId.parentId))
        num += mResolver.update(mUris.documentAudio(), values,
                "tree_uri=? AND document_id=? AND parent_id=?",
                arrayOf(documentId.treeUri.toString(), documentId.documentId, documentId.parentId))
        return num != 0
    }

    /*
     * END DOCUMENTS
     */

    /*
     * START TV
     */

    fun addTvSeries(series: TvSeriesRef): Boolean {
        return mResolver.insert(mUris.tvSeries(), series.contentValues()) == URI_SUCCESS
    }

    fun getTvSeries(seriesId: TvSeriesId): Maybe<TvSeriesRef> {
        return Maybe.create { s ->
            mResolver.query(mUris.tvSeries(), tvSeriesProjection,
                    "_id=?", arrayOf(seriesId.seriesId.toString()), null, null)?.use { c ->
                if (c.moveToFirst()) {
                    s.onSuccess(c.toTvSeries())
                } else {
                    s.onComplete()
                }
            } ?: s.onError(VideoDatabaseMalfuction())
        }
    }

    fun addTvEpisodes(episodes: List<TvEpisodeRef>): Int {
        val values = Array(episodes.size, { idx ->
            episodes[idx].contentValues()
        })
        return mResolver.bulkInsert(mUris.tvEpisode(), values)
    }

    fun getTvEpisodesForTvSeries(seriesId: TvSeriesId): Observable<TvEpisodeRef> {
        return doQuery(mUris.tvEpisode(), tvEpisodesProjection,
                "series_id=?", arrayOf(seriesId.seriesId.toString()),
                null, { c ->c.toTvEpisodeMediaMeta() })
    }

    fun getTvEpisode(episodeId: TvEpisodeId): Maybe<TvEpisodeRef> {
        return Maybe.create { s ->
            mResolver.query(mUris.tvEpisode(), tvEpisodesProjection,
                    "_id=?", arrayOf(episodeId.episodeId.toString()), null, null)?.use { c ->
                if (c.moveToFirst()) {
                    s.onSuccess(c.toTvEpisodeMediaMeta())
                } else {
                    s.onComplete()
                }
            } ?: s.onError(VideoDatabaseMalfuction())
        }
    }

    fun addTvImages(banners: List<TvImageRef>): Int {
        val values = Array(banners.size, { idx ->
            banners[idx].contentValues()
        })
        return mResolver.bulkInsert(mUris.tvImage(), values)
    }

    fun getTvPosters(seriesId: TvSeriesId): Observable<TvImageRef> {
        return doQuery(mUris.tvImage(), tvBannerProjection,
                "series_id=? and type=?", arrayOf(seriesId.seriesId.toString(), "poster"),
                "rating DESC", { c ->c.toTvBannerMediaMeta() })
    }

    fun getTvBackdrops(seriesId: TvSeriesId): Observable<TvImageRef> {
        return doQuery(mUris.tvImage(), tvBannerProjection,
                "series_id=? and type=?", arrayOf(seriesId.seriesId.toString(), "fanart"),
                "rating DESC", { c ->c.toTvBannerMediaMeta() })
    }

    /*
     * END TV
     */

    /*
     * START MOVIE
     */

    fun addMovie(movie: MovieRef): Boolean {
        return mResolver.insert(mUris.movie(), movie.contentValues()) == URI_SUCCESS
    }

    fun getMovie(movieId: MovieId): Maybe<MovieRef> {
        return Maybe.create { s ->
            mResolver.query(mUris.movie(), movieProjection,
                    "_id=?", arrayOf(movieId.movieId.toString()), null, null )?.use { c ->
                if (c.moveToFirst()) {
                    s.onSuccess(c.toMovieRef())
                } else {
                    s.onComplete()
                }
            } ?: s.onError(VideoDatabaseMalfuction())
        }
    }

    fun addMovieImages(images: List<MovieImageRef>): Int {
        val contentValues = Array(images.size, { idx ->
            images[idx].contentValues()
        })
        return mResolver.bulkInsert(mUris.movieImage(), contentValues)
    }

    fun getMoviePosters(movieId: MovieId): Observable<MovieImageRef> {
        TODO()
    }

    fun getMovieBackdrops(movieId: MovieId): Observable<MovieImageRef> {
        TODO()
    }

}

fun UpnpDeviceRef.contentValues(): ContentValues {
    val cv = ContentValues()
    val meta = this
    cv.put("device_id", meta.id.deviceId)
    cv.put("title", meta.meta.title)
    cv.put("subtitle", meta.meta.subtitle)
    cv.put("artwork_uri", if (meta.meta.artworkUri.isEmpty()) "" else meta.meta.artworkUri.toString())
    cv.put("available", 1)
    cv.put("scanning", 0) //reset here
    return cv
}

val upnpDeviceProjection = arrayOf("device_id", "title", //1
        "subtitle", "artwork_uri", "update_id") //4

fun Cursor.toUpnpDeviceRef(): UpnpDeviceRef {
    val mediaId = UpnpDeviceId(getString(0))
    val title = getString(1)
    val subtitle = getString(2) ?: ""
    val artworkUri = if (!isNull(3)) Uri.parse(getString(3)) else Uri.EMPTY
    val updateId = getLong(4)
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

fun UpnpFolderRef.contentValues(): ContentValues {
    val cv = ContentValues()
    val meta = this
    cv.put("device_id", meta.id.deviceId)
    cv.put("folder_id", meta.id.containerId)
    cv.put("parent_id", meta.id.parentId)
    cv.put("_display_name", meta.meta.title)
    cv.put("hidden", 0)
    return cv
}

val upnpFolderProjection = arrayOf("device_id", "folder_id",
        "parent_id", "_display_name")

fun Cursor.toUpnpFolderRef(): UpnpFolderRef {
    val mediaId = UpnpFolderId(getString(0), getString(2), getString(1))
    val title = getString(3)
    return UpnpFolderRef(
            mediaId,
            UpnpFolderMeta(
                    title = title
            )
    )
}

fun UpnpVideoRef.contentValues(): ContentValues {
    val cv = ContentValues()
    val meta = this
    cv.put("device_id", meta.id.deviceId)
    cv.put("item_id", meta.id.itemId)
    cv.put("parent_id", meta.id.parentId)
    cv.put("_display_name", meta.meta.originalTitle.elseIfBlank(meta.meta.title))
    cv.put("mime_type", meta.meta.mimeType)
    cv.put("media_uri", meta.meta.mediaUri.toString())
    cv.put("duration", meta.meta.duration)
    cv.put("bitrate", meta.meta.bitrate)
    cv.put("file_size", meta.meta.size)
    cv.put("resolution", meta.meta.resolution)
    cv.put("date_added", System.currentTimeMillis())
    cv.put("hidden", 0)
    return cv
}

val upnpVideoProjection = arrayOf(
        //upnp_video columns
        "device_id", "device_id", "item_id", "parent_id", //3
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
fun Cursor.toUpnpVideoMediaMeta(mApiHelper: ApiHelper): UpnpVideoRef {
    val c = this
    val mediaId = UpnpVideoId(c.getString(1), c.getString(3), c.getString(2))
    var title = c.getString(4)
    val originalTitle = title
    val mimeType = c.getString(5)
    val mediaUri = Uri.parse(c.getString(6))
    val duration = c.getLong(7)
    val size = c.getLong(8)
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
            artworkUri = mApiHelper.tvImagePosterUri(c.getString(21))
        } else if (!c.isNull(15)) {
            artworkUri = mApiHelper.tvImagePosterUri(c.getString(15))
        }
        if (!c.isNull(22)) {
            backdropUri = mApiHelper.tvImageBackdropUri(c.getString(22))
        } else if (!c.isNull(16)) {
            backdropUri = mApiHelper.tvImageBackdropUri(c.getString(16))
        }
    } else if (!c.isNull(17)) {
        movieId = MovieId(c.getLong(17))
        title = c.getString(18)
        if (!c.isNull(19)) {
            artworkUri = mApiHelper.movieImagePosterUri(c.getString(19))
        }
        if (!c.isNull(20)) {
            backdropUri = mApiHelper.movieImagePosterUri(c.getString(20))
        }
    }
    return UpnpVideoRef(
            id = mediaId,
            tvEpisodeId =  episodeId,
            movieId = movieId,
            meta = UpnpVideoMeta(
                    title = title,
                    subtitle = subtitle,
                    artworkUri = artworkUri,
                    backdropUri = backdropUri,
                    originalTitle = originalTitle,
                    mediaUri = mediaUri,
                    mimeType = mimeType,
                    duration = duration,
                    size = size
                    //resolution =
            )
    )
}

fun DirectoryDocumentRef.contentValues(): ContentValues {
    val values = ContentValues()
    values.put("authority", id.treeUri.authority)
    values.put("tree_uri", id.treeUri.toString())
    values.put("document_id", id.documentId)
    values.put("parent_id", id.parentId)
    values.put("_display_name", meta.displayName)
    values.put("mime_type", meta.mimeType)
    values.put("flags", meta.flags)
    values.put("last_modified", meta.lastMod)
    values.put("hidden", 0)
    return values
}

val directoryDocumentProjection = arrayOf(
        //document columns
        "tree_uri", "document_id", "parent_id", //2
        "mime_type", "_display_name", "flags", "last_modified" //6
)

fun Cursor.toDirectoryDocument(): DirectoryDocumentRef {
    val id = DocumentId(Uri.parse(getString(0)), getString(1), getString(2), getString(3))
    val mimetype = getString(3)
    val displayname = getString(4)
    val flags = if (!isNull(5)) getLong(5) else 0L
    val lastmod = if (!isNull(6)) getLong(6) else 0L
    return DirectoryDocumentRef(
            id = id,
            meta = DocumentMeta(
                    displayName = displayname,
                    mimeType = mimetype,
                    flags = flags,
                    lastMod = lastmod
            )
    )
}

fun VideoDocumentRef.contentValues(): ContentValues {
    val values = ContentValues()
    val documentRef = this
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
    return values
}

val videoDocumentProjection = arrayOf(
        //document columns
        "tree_uri", "tree_uri", "document_id", "parent_id", //3
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
fun Cursor.toVideoDocumentRef(mApiHelper: ApiHelper): VideoDocumentRef {
    val treeUri = Uri.parse(getString(1))
    val docId = getString(2)
    val parentId = getString(3)
    val displayName = getString(4)
    var title = displayName
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
            artworkUri = mApiHelper.tvImagePosterUri(getString(21))
        } else if (!isNull(15)) {
            artworkUri = mApiHelper.tvImagePosterUri(getString(15))
        }
        if (!isNull(22)) {
            backdropUri = mApiHelper.tvImageBackdropUri(getString(22))
        } else if (!isNull(16)) {
            backdropUri = mApiHelper.tvImageBackdropUri(getString(16))
        }
    } else if (!isNull(17)) {
        movieId = MovieId(getLong(17))
        title = getString(18)
        if (!isNull(19)) {
            artworkUri = mApiHelper.movieImagePosterUri(getString(19))
        }
        if (!isNull(20)) {
            backdropUri = mApiHelper.movieImageBackdropUri(getString(20))
        }
    }
    return VideoDocumentRef(
            id = DocumentId(
                    treeUri = treeUri,
                    documentId = docId,
                    parentId = parentId,
                    mimeType = mimeType
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

fun TvSeriesRef.contentValues(): ContentValues {
    val values = ContentValues(10)
    val series = this
    values.put("_id", series.id.seriesId)
    values.put("_display_name", series.meta.title)
    values.put("overview", series.meta.overview)
    values.put("first_aired", series.meta.releaseDate)
    values.put("poster", series.meta.posterPath)
    values.put("backdrop", series.meta.backdropPath)
    return values
}

val tvSeriesProjection = arrayOf("_display_name", "overview",
        "first_aired", "_id", "poster", "backdrop")

fun Cursor.toTvSeries(): TvSeriesRef {
    val c = this
    return TvSeriesRef(
            TvSeriesId(c.getLong(3)),
            TvSeriesMeta(
                    title = c.getString(0),
                    overview = c.getString(1) ?: "",
                    releaseDate = c.getString(2),
                    posterPath = c.getString(4) ?: "",
                    backdropPath = c.getString(5) ?: ""
            )
    )
}

fun TvEpisodeRef.contentValues(): ContentValues {
    val values = ContentValues()
    val episode = this
    values.put("_id", episode.id.episodeId)
    values.put("series_id", episode.id.seriesId)
    values.put("_display_name", episode.meta.title)
    values.put("overview", episode.meta.overview)
    values.put("first_aired", episode.meta.releaseDate)
    values.put("episode_number", episode.meta.episodeNumber)
    values.put("season_number", episode.meta.seasonNumber)
    values.put("poster", episode.meta.posterPath)
    values.put("backdrop", episode.meta.backdropPath)
    return values
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

fun TvImageRef.contentValues(): ContentValues {
    val values = ContentValues()
    val banner = this
    values.put("_id", banner.id.imageId)
    values.put("series_id", banner.id.seriesId)
    values.put("path", banner.meta.path)
    values.put("type", banner.meta.type)
    values.put("type2", banner.meta.subType)
    values.put("rating", banner.meta.rating)
    values.put("rating_count", banner.meta.ratingCount)
    values.put("resolution", banner.meta.resolution)
    return values
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

fun MovieRef.contentValues(): ContentValues {
    val values = ContentValues(10)
    values.put("_id", this.id.movieId)
    values.put("_display_name", this.meta.title)
    values.put("overview", this.meta.overview)
    values.put("release_date", this.meta.releaseDate)
    values.put("poster_path", this.meta.posterPath)
    values.put("backdrop_path", this.meta.backdropPath)
    return values
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

fun MovieImageRef.contentValues(): ContentValues {
    val values = ContentValues()
    val image = this
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

fun makeTvSubtitle(seriesName: String, seasonNumber: Int, episodeNumber: Int): String {
    return "$seriesName - S${seasonNumber.zeroPad(2)}E${episodeNumber.zeroPad(2)}"
}

fun Int.zeroPad(len: Int): String {
    return this.toString().padStart(len, '0')
}

/**
 * myriad scale (0-1000)
 */
fun calculateCompletion(current: Long, duration: Long): Int {
    //permyriad calculation (no floating point)
    return ((1000*current + duration/2)/duration).toInt()
}
