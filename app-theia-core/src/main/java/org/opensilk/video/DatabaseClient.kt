package org.opensilk.video

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.database.ContentObserver
import android.database.Cursor
import android.database.sqlite.SQLiteException
import android.media.browse.MediaBrowser
import android.net.Uri
import android.os.CancellationSignal
import com.google.android.exoplayer2.util.MimeTypes
import dagger.Binds
import dagger.Module
import org.fourthline.cling.model.meta.Device
import org.fourthline.cling.model.meta.RemoteDeviceIdentity
import org.opensilk.common.dagger.ForApplication
import org.opensilk.media.*
import org.opensilk.media.playback.MediaProviderClient
import org.opensilk.tmdb.api.model.Image
import org.opensilk.tmdb.api.model.ImageList
import org.opensilk.tmdb.api.model.Movie
import org.opensilk.tmdb.api.model.TMDbConfig
import org.opensilk.tvdb.api.model.*
import rx.Single
import rx.Observable
import rx.Subscriber
import rx.Subscription
import rx.lang.kotlin.BehaviorSubject
import rx.subscriptions.Subscriptions
import timber.log.Timber
import java.util.*
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Module
abstract class MediaProviderModule {
    @Binds abstract fun providerClient(providerClient: DatabaseClient): MediaProviderClient
}

/**
 *
 */
class NoSuchItemException: Exception()

fun Int.zeroPad(len: Int): String {
    return this.toString().padStart(len, '0')
}

/**
 * A bridge for testing
 */
internal interface ContentResolverGlue {
    fun insert(uri: Uri, values: ContentValues): Uri?
    fun bulkInsert(uri: Uri, values: Array<ContentValues>): Int
    fun update(uri: Uri, values: ContentValues, where: String?,
               selectionArgs: Array<String>?): Int
    fun query(uri: Uri, projection: Array<String>?, selection: String?,
              selectionArgs: Array<String>?, sortOrder: String?,
              cancellationSignal: CancellationSignal?): Cursor?
    fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int
    fun notifyChange(uri: Uri, co: ContentObserver?)
}

/**
 * The default implementation passes through to the system ContentResolver
 */
private class DefaultContentResolverGlue(private val mResolver: ContentResolver): ContentResolverGlue {
    override fun insert(uri: Uri, values: ContentValues): Uri? {
        return mResolver.insert(uri, values)
    }

    override fun bulkInsert(uri: Uri, values: Array<ContentValues>): Int {
        return mResolver.bulkInsert(uri, values)
    }

    override fun update(uri: Uri, values: ContentValues, where: String?,
                        selectionArgs: Array<String>?): Int {
        return mResolver.update(uri, values, where, selectionArgs)
    }

    override fun query(uri: Uri, projection: Array<String>?, selection: String?,
                       selectionArgs: Array<String>?, sortOrder: String?,
                       cancellationSignal: CancellationSignal?): Cursor? {
        return mResolver.query(uri, projection, selection, selectionArgs,
                sortOrder, cancellationSignal)
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int {
        return mResolver.delete(uri, selection, selectionArgs)
    }

    override fun notifyChange(uri: Uri, co: ContentObserver?) {
        mResolver.notifyChange(uri, co)
    }
}

fun <T> Subscriber<T>.cancellationSignal(): CancellationSignal {
    val cancelation = CancellationSignal()
    this.add(Subscriptions.create { cancelation.cancel() })
    return cancelation
}

class VideoDatabaseMalfuction: Exception()

interface DatabaseChange
class UpnpDeviceChange: DatabaseChange
class UpnpFolderChange(val folderId: UpnpFolderId): DatabaseChange
class UpnpVideoChange(val videoId: UpnpVideoId): DatabaseChange

/**
 * Created by drew on 7/18/17.
 */
@Singleton
class DatabaseClient
@Inject constructor(
    @ForApplication context: Context,
    private val mUris: DatabaseUris,
    @Named("tvdb_banner_root") tvdbRootUri: Uri
) : MediaProviderClient {

    internal var mResolver: ContentResolverGlue = DefaultContentResolverGlue(context.contentResolver)
    val tvdb = TVDbClient(tvdbRootUri)
    val tmdb = MovieDbClient()
    val uris = mUris
    val moviedb = tmdb

    private val mChangesSubject = BehaviorSubject<DatabaseChange>()

    val changesObservable: Observable<DatabaseChange>
        get() = mChangesSubject.asObservable()

    fun postChange(event: DatabaseChange) {
        mChangesSubject.onNext(event)
    }

    override fun getMediaItem(mediaRef: MediaRef): Single<MediaBrowser.MediaItem> {
        return when (mediaRef.kind) {
            UPNP_FOLDER -> getUpnpFolder(mediaRef.mediaId as UpnpFolderId).map { it.toMediaItem() }
            UPNP_VIDEO -> getUpnpVideo(mediaRef.mediaId as UpnpVideoId).map { it.toMediaItem() }
            UPNP_DEVICE -> getUpnpDevice(mediaRef.mediaId as UpnpDeviceId).map { it.toMediaItem() }
            else -> TODO()
        }
    }

    fun getVideoDescription(mediaRef: MediaRef): Single<VideoDescInfo> {
        return Single.create { s ->
            s.onError(NoSuchItemException())
        }
    }

    /**
     * Add a meta item describing a upnp device with a content directory service to the database
     * item should be created with Device.toMediaMeta
     */
    fun addUpnpDevice(meta: MediaMeta): Uri {
        val id = newMediaRef(meta.mediaId)
        if (id.kind != UPNP_DEVICE) throw IllegalArgumentException("media.kind not UPNP_DEVICE")
        val cv = ContentValues()
        cv.put("device_id", (id.mediaId as UpnpDeviceId).deviceId)
        cv.put("mime_type", meta.mimeType)
        cv.put("title", meta.title)
        cv.put("subtitle", meta.subtitle)
        if (meta.artworkUri != Uri.EMPTY) {
            cv.put("artwork_uri", meta.artworkUri.toString())
        }
        cv.put("available", 1)
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
    fun getUpnpDevices(): Observable<MediaMeta> {
        return Observable.create { s ->
            mResolver.query(mUris.upnpDevices(), upnpDeviceProjection,
                    "available=1", null, "title", s.cancellationSignal())?.use { c ->
                while (c.moveToNext()) {
                    s.onNext(c.toUpnpDeviceMediaMeta())
                }
                s.onCompleted()
            } ?: s.onError(VideoDatabaseMalfuction())
        }
    }

    fun getUpnpDevice(deviceId: UpnpDeviceId): Single<MediaMeta> {
        return Single.create { s ->
            mResolver.query(mUris.upnpDevices(), upnpDeviceProjection,
                    "device_id=?", arrayOf(deviceId.deviceId), null, null)?.use { c ->
                if (c.moveToFirst()) {
                    s.onSuccess(c.toUpnpDeviceMediaMeta())
                } else {
                    s.onError(NoSuchItemException())
                }
            } ?: s.onError(VideoDatabaseMalfuction())
        }
    }

    val upnpDeviceProjection = arrayOf("_id", "device_id", "mime_type", "title",
            "subtitle", "artwork_uri")

    fun Cursor.toUpnpDeviceMediaMeta(): MediaMeta {
        val c = this
        val meta = MediaMeta()
        meta.rowId = c.getLong(0)
        meta.mediaId = MediaRef(UPNP_DEVICE, UpnpDeviceId(c.getString(1))).toJson()
        meta.mimeType = c.getString(2)
        meta.title = c.getString(3) ?: ""
        meta.subtitle = c.getString(4) ?: ""
        if (!c.isNull(5)) meta.artworkUri = Uri.parse(c.getString(5))
        return meta
    }

    /**
     * add a upnp folder to the database, item should be created with Container.toMediaMeta
     */
    fun addUpnpFolder(meta: MediaMeta): Uri {
        val id = newMediaRef(meta.mediaId)
        if (id.kind != UPNP_FOLDER) throw IllegalArgumentException("meta.kind not UPNP_FOLDER")
        val parentId = newMediaRef(meta.parentMediaId)
        val cv = ContentValues()
        cv.put("device_id", (id.mediaId as UpnpFolderId).deviceId)
        cv.put("folder_id", (id.mediaId as UpnpFolderId).folderId)
        cv.put("parent_id", (parentId.mediaId as UpnpFolderId).folderId)
        cv.put("_display_name", meta.title)
        if (meta.artworkUri != Uri.EMPTY) {
            cv.put("artwork_uri", meta.artworkUri.toString())
        }
        cv.put("mime_type", meta.mimeType)
        cv.put("date_added", System.currentTimeMillis())
        return mResolver.insert(uris.upnpFolders(), cv) ?: Uri.EMPTY
    }

    /**
     * removes specified folder from database
     */
    fun removeUpnpFolder(rowid: Long): Boolean {
        return mResolver.delete(uris.upnpFolder(rowid), null, null) != 0
    }

    /**
     * retrieve direct decedents of parent folder
     */
    fun getUpnpFolders(parentId: UpnpFolderId): Observable<MediaMeta> {
        return Observable.create { s ->
            mResolver.query(mUris.upnpFolders(), upnpFolderProjection,
                    "device_id=? AND parent_id=?",
                    arrayOf(parentId.deviceId, parentId.folderId),
                    "_display_name", s.cancellationSignal())?.use { c ->
                while (c.moveToNext()) {
                    s.onNext(c.toUpnpFolderMediaMeta())
                }
                s.onCompleted()
            } ?: s.onError(VideoDatabaseMalfuction())
        }
    }

    fun getUpnpFolder(folderId: UpnpFolderId): Single<MediaMeta> {
        return Single.create { s ->
            mResolver.query(uris.upnpFolders(), upnpFolderProjection,
                    "device_id=? AND folder_id=?",
                    arrayOf(folderId.deviceId, folderId.folderId), null, null)?.use { c ->
                if (c.moveToFirst()) {
                    s.onSuccess(c.toUpnpFolderMediaMeta())
                } else {
                    s.onError(NoSuchItemException())
                }
            } ?: s.onError(VideoDatabaseMalfuction())
        }
    }

    val upnpFolderProjection = arrayOf("_id", "device_id", "folder_id",
            "parent_id", "_display_name", "artwork_uri", "mime_type")

    fun Cursor.toUpnpFolderMediaMeta(): MediaMeta {
        val c = this
        val meta = MediaMeta()
        meta.rowId = c.getLong(0)
        meta.mediaId = MediaRef(UPNP_FOLDER, UpnpFolderId(c.getString(1), c.getString(2))).toJson()
        meta.parentMediaId = MediaRef(UPNP_FOLDER, UpnpFolderId(c.getString(1), c.getString(3))).toJson()
        meta.title = c.getString(4)
        if (!c.isNull(5)) meta.artworkUri = Uri.parse(c.getString(5))
        meta.mimeType = c.getString(6)
        return meta
    }

    /**
     * Adds upnp video to database, item should be created with VideoItem.toMediaMeta
     */
    fun addUpnpVideo(meta: MediaMeta): Uri {
        val id = newMediaRef(meta.mediaId)
        if (id.kind != UPNP_VIDEO) throw IllegalArgumentException("media.kind not UPNP_VIDEO")
        val parentId = newMediaRef(meta.parentMediaId)
        val cv = ContentValues()
        cv.put("device_id", (id.mediaId as UpnpVideoId).deviceId)
        cv.put("item_id", (id.mediaId as UpnpVideoId).itemId)
        cv.put("parent_id", (parentId.mediaId as UpnpFolderId).folderId)
        cv.put("_display_name", meta.displayName)
        cv.put("mime_type", meta.mimeType)
        cv.put("media_uri", meta.mediaUri.toString())
        cv.put("duration", meta.duration)
        cv.put("bitrate", meta.bitrate)
        cv.put("file_size", meta.size)
        cv.put("resolution", meta.resolution)
        cv.put("date_added", System.currentTimeMillis())
        return mResolver.insert(uris.upnpVideos(), cv) ?: Uri.EMPTY
    }

    /**
     * remove specified video from database
     */
    fun removeUpnpVideo(id: Long): Boolean {
        return mResolver.delete(uris.upnpVideo(id), null, null) != 0
    }

    /**
     * retrieve upnp videos, direct decedents of parent
     */
    fun getUpnpVideos(parentId: UpnpFolderId): Observable<MediaMeta> {
        return Observable.create { s ->
            mResolver.query(mUris.upnpVideos(), upnpVideoProjection,
                    "device_id=? AND parent_id=?", arrayOf(parentId.deviceId, parentId.folderId),
                    "v._display_name", s.cancellationSignal())?.use { c ->
                val baseUrl = moviedb.getConfig().images.baseUrl
                while (c.moveToNext()) {
                    s.onNext(c.toUpnpVideoMediaMeta(baseUrl))
                }
                s.onCompleted()
            } ?: s.onError(VideoDatabaseMalfuction())
        }
    }

    /**
     * retrieve specified upnp video
     */
    fun getUpnpVideo(id: Long): Single<MediaMeta> {
        return Single.create { s ->
            mResolver.query(mUris.upnpVideo(id), upnpVideoProjection,
                    null, null, null, null)?.use { c ->
                if (c.moveToFirst()) {
                    s.onSuccess(c.toUpnpVideoMediaMeta(moviedb.getConfig().images.baseUrl))
                } else {
                    s.onError(NoSuchItemException())
                }
            } ?: s.onError(VideoDatabaseMalfuction())
        }
    }

    fun getUpnpVideo(videoId: UpnpVideoId): Single<MediaMeta> {
        return Single.create { s ->
            mResolver.query(uris.upnpVideos(), upnpVideoProjection,
                    "device_id=? AND item_id=?",
                    arrayOf(videoId.deviceId, videoId.itemId), null, null)?.use { c ->
                if (c.moveToFirst()) {
                    s.onSuccess(c.toUpnpVideoMediaMeta(moviedb.getConfig().images.baseUrl))
                } else {
                    s.onError(NoSuchItemException())
                }
            } ?: s.onError(VideoDatabaseMalfuction())
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
            //movie columns
            "m._id as movie_id", "m._display_name as movie_title",  //16
            "m.poster_path as movie_poster", //17
            "m.backdrop_path as movie_backdrop") //18

    /**
     * helper to convert cursor to mediameta using above projection
     */
    fun Cursor.toUpnpVideoMediaMeta(baseUrl: String): MediaMeta {
        val c = this
        val meta = MediaMeta()
        meta.rowId = c.getLong(0)
        meta.mediaId = MediaRef(UPNP_VIDEO, UpnpVideoId(c.getString(1), c.getString(2))).toJson()
        meta.parentMediaId = MediaRef(UPNP_FOLDER, UpnpFolderId(c.getString(1), c.getString(3))).toJson()
        meta.displayName = c.getString(4)
        meta.title = c.getString(4)
        meta.mimeType = c.getString(5)
        meta.mediaUri = Uri.parse(c.getString(6))
        meta.duration = c.getLong(7)
        meta.size = c.getLong(8)
        if (!c.isNull(9)) {
            meta.extras.putLong("episode", c.getLong(9))
            meta.extras.putLong("series", c.getLong(13))
            meta.title = c.getString(10)
            meta.subtitle = tvdb.makeSubtitle(c.getString(14), c.getInt(11), c.getInt(12))
        } else if (!c.isNull(15)) {
            meta.extras.putLong("movie", c.getLong(15))
            meta.title = c.getString(16)
            if (!c.isNull(17)) {
                meta.artworkUri = tmdb.makePosterUri(baseUrl, c.getString(19))
            }
            if (!c.isNull(18)) {
                meta.backdropUri = tmdb.makeBackdropUri(baseUrl, c.getString(20))
            }
        }
        return meta
    }

    inner class TVDbClient(private val tvdbRoot: Uri) {

        fun rootUri(): Uri {
            return tvdbRoot
        }

        fun bannerRootUri(): Uri {
            return Uri.withAppendedPath(tvdbRoot, "banners/")
        }

        fun makeBannerUri(path: String): Uri {
            return Uri.withAppendedPath(bannerRootUri(), path)
        }

        fun makeSubtitle(seriesName: String, seasonNumber: Int, episodeNumber: Int): String {
            return "$seriesName - S${seasonNumber.zeroPad(2)}E${episodeNumber.zeroPad(2)}"
        }

        fun setLastUpdate(lastUpdate: Long) {
            val cv = ContentValues()
            cv.put("key", "last_update")
            cv.put("value", lastUpdate)
            mResolver.insert(mUris.tvConfig(), cv)
        }

        fun getLastUpdate(): Single<Long> {
            return Single.create { s ->
                mResolver.query(mUris.tvConfig(), arrayOf("value"),
                        "key='last_update'", null, null, null)?.use { c ->
                    if (c.moveToFirst()) {
                        s.onSuccess(c.getString(0).toLong())
                    } else {
                        s.onSuccess(0)
                    }
                } ?: s.onError(VideoDatabaseMalfuction())
            }
        }

        fun setToken(token: Token) {
            val cv = ContentValues()
            cv.put("key", "token")
            cv.put("value", token.token)
            mResolver.insert(mUris.tvConfig(), cv)
        }

        fun getToken(): Single<Token> {
            return Single.create { s ->
                mResolver.query(mUris.tvConfig(), arrayOf("value"),
                        "key='token'", null, null, null)?.use { c ->
                    if (c.moveToFirst()) {
                        s.onSuccess(Token(c.getString(0)))
                    } else {
                        s.onError(NoSuchItemException())
                    }
                } ?: s.onError(VideoDatabaseMalfuction())
            }
        }

        fun addTvSeries(series: Series): Uri {
            val values = ContentValues(10)
            values.put("_id", series.id)
            values.put("_display_name", series.seriesName)
            values.put("overview", series.overview)
            values.put("first_aired", series.firstAired)
            values.put("banner", series.banner)
            return mResolver.insert(mUris.tvSeries(), values) ?: Uri.EMPTY
        }

        fun getTvSeries(seriesId: Long): Single<MediaMeta> {
            return Single.create { s ->
                mResolver.query(mUris.tvSeries(),
                        arrayOf("_display_name", "overview", "first_aired", "_id"),
                        "_id=?", arrayOf(seriesId.toString()), null, null)?.use { c ->
                    if (c.moveToFirst()) {
                        val meta = MediaMeta()
                        meta.mimeType = MIME_TYPE_TV_SERIES
                        meta.title = c.getString(0)
                        meta.overview = c.getString(1)
                        meta.releaseDate = c.getString(2)
                        meta.rowId = c.getLong(3)
                        s.onSuccess(meta)
                    } else {
                        s.onError(NoSuchItemException())
                    }
                } ?: s.onError(VideoDatabaseMalfuction())
            }
        }

        fun addTvEpisode(seriesId: Long, episode: SeriesEpisode): Uri {
            val values = ContentValues(10)
            values.put("_id", episode.id)
            values.put("_display_name", episode.episodeName)
            values.put("overview", episode.overview)
            values.put("first_aired", episode.firstAired)
            values.put("episode_number", episode.airedEpisodeNumber)
            values.put("season_number", episode.airedSeason)
            values.put("series_id", seriesId)
            return mResolver.insert(mUris.tvEpisodes(), values) ?: Uri.EMPTY
        }

        fun getTvEpisodes(seriesId: Long): Observable<MediaMeta> {
            return Observable.create { s ->
                mResolver.query(mUris.tvEpisodes(), tvEpisodesProjection,
                        "series_id=?", arrayOf(seriesId.toString()),
                        null, s.cancellationSignal())?.use { c ->
                    while (c.moveToNext()) {
                        s.onNext(c.toTvEpisodeMediaMeta())
                    }
                    s.onCompleted()
                } ?: s.onError(VideoDatabaseMalfuction())
            }
        }

        fun getTvEpisode(episodeId: Long): Single<MediaMeta> {
            return Single.create { s ->
                mResolver.query(mUris.tvEpisode(episodeId), tvEpisodesProjection,
                        null, null, null, null)?.use { c ->
                    if (c.moveToFirst()) {
                        s.onSuccess(c.toTvEpisodeMediaMeta())
                    } else {
                        s.onError(NoSuchItemException())
                    }
                } ?: s.onError(VideoDatabaseMalfuction())
            }
        }

        val tvEpisodesProjection = arrayOf(
                "_id", "_display_name", "first_aired",
                "episode_number", "season_number",
                "overview")

        fun Cursor.toTvEpisodeMediaMeta(): MediaMeta {
            val meta = MediaMeta()
            meta.mimeType = MIME_TYPE_TV_EPISODE
            meta.rowId = getLong(0)
            meta.title = getString(1)
            meta.releaseDate = getString(2)
            meta.episodeNumber = getInt(3)
            meta.seasonNumber = getInt(4)
            if (!isNull(5)) {
                meta.overview = getString(5)
            }
            return meta
        }

        fun insertTvBanner(seriesId: Long, banner: SeriesImageQuery): Uri {
            val values = ContentValues(10)
            values.put("_id", banner.id)
            values.put("path", banner.fileName)
            values.put("type", banner.keyType)
            values.put("type2", banner.subKey)
            values.put("rating", banner.ratingsInfo.average)
            values.put("rating_count", banner.ratingsInfo.count)
            values.put("thumb_path", banner.thumbnail)
            values.put("resolution", banner.resolution)
            values.put("series_id", seriesId)
            return mResolver.insert(mUris.tvBanners(), values) ?: Uri.EMPTY
        }

        fun getTvBanners(seriesId: Long): rx.Observable<MediaMeta> {
            return rx.Observable.create { s ->
                mResolver.query(mUris.tvBanners(), tvBannerProjection,
                        "series_id=?", arrayOf(seriesId.toString()),
                        "rating DESC", s.cancellationSignal())?.use { c ->
                    while (c.moveToNext()) {
                        s.onNext(c.toTvBannerMediaMeta())
                    }
                    s.onCompleted()
                } ?: s.onError(VideoDatabaseMalfuction())
            }
        }

        val tvBannerProjection = arrayOf("_id", "path", "type", "type2")

        fun Cursor.toTvBannerMediaMeta(): MediaMeta {
            val meta = MediaMeta()
            meta.mimeType = MIME_TYPE_JPEG
            meta.rowId = getLong(0)
            meta.artworkUri = makeBannerUri(getString(1))
            meta.extras.putString("type", getString(2))
            meta.extras.putString("type2", getString(3))
            return meta
        }

        fun getSeriesAssociation(query: String): Single<Long> {
            return Single.create { s ->
                mResolver.query(mUris.tvLookups(),
                        arrayOf("series_id"), "q=?", arrayOf(query), null, null)?.use { c ->
                    if (c.moveToFirst()) {
                        s.onSuccess(c.getLong(0))
                    } else {
                        s.onError(NoSuchItemException())
                    }
                } ?: s.onError(VideoDatabaseMalfuction())
            }
        }

        fun setSeriesAssociation(query: String, series_id: Long) {
            val cv = ContentValues(2)
            cv.put("q", query)
            cv.put("series_id", series_id)
            val uri = mResolver.insert(mUris.tvLookups(), cv)
            //
        }

    }

    inner class MovieDbClient {

        fun makePosterUri(base: String, path: String): Uri {
            return Uri.parse("${base}w342$path")
        }

        fun makeBackdropUri(base: String, path: String): Uri {
            return Uri.parse("${base}w1280$path")
        }

        @Synchronized
        fun setConfig(config: TMDbConfig): Boolean {
            val values = Array(1, { ContentValues() })
            values[0].put("key", "image_base_url")
            values[0].put("value", config.images.baseUrl)
            return mResolver.bulkInsert(mUris.movieConfig(), values) != 0
        }

        @Synchronized
        fun getConfig(): TMDbConfig {
            return mResolver.query(mUris.movieConfig(), arrayOf("key", "value"),
                    null, null, null, null)?.use { c ->
                var baseUrl = ""
                while (c.moveToNext()) {
                    when (c.getString(0)) {
                        "image_base_url" -> baseUrl = c.getString(1)
                    }
                }
                return@use TMDbConfig(TMDbConfig.Images(
                        baseUrl, null, null, null, null, null, null
                ))
            } ?: throw VideoDatabaseMalfuction()
        }


        fun addMovie(movie: Movie): Uri {
            val values = ContentValues(10)
            values.put("_id", movie.id)
            values.put("_display_name", movie.title)
            values.put("overview", movie.overview)
            values.put("release_date", movie.releaseDate)
            values.put("poster_path", movie.posterPath)
            values.put("backdrop_path", movie.backdropPath)
            return mResolver.insert(mUris.movies(), values) ?: Uri.EMPTY
        }

        fun getMovie(movieId: Long): Single<MediaMeta> {
            return Single.create { s ->
                mResolver.query(mUris.movie(movieId), arrayOf("_display_name", "overview",
                        "release_date", "poster_path", "backdrop_path", "_id"), null,
                        null, null, null )?.use { c ->
                    if (c.moveToFirst()) {
                        val baseUrl = getConfig().images.baseUrl
                        val meta = MediaMeta()
                        meta.mimeType = MIME_TYPE_MOVIE
                        meta.title = c.getString(0)
                        meta.overview = c.getString(1)
                        meta.releaseDate = c.getString(2)
                        if (!c.isNull(3)) {
                            meta.artworkUri = makePosterUri(baseUrl, c.getString(3))
                        }
                        if (!c.isNull(4)) {
                            meta.backdropUri = makeBackdropUri(baseUrl, c.getString(4))
                        }
                        meta.rowId = c.getLong(5)
                        s.onSuccess(meta)
                    }
                }
            }
        }

        fun addMovieImages(imageList: ImageList) {
            val numPosters = if (imageList.posters != null) imageList.posters.size else 0
            val numBackdrops = if (imageList.posters != null) imageList.backdrops.size else 0
            val contentValues = Array(numPosters + numBackdrops, { ContentValues() })
            var idx = 0
            if (numPosters > 0) {
                for (image in imageList.posters) {
                    val values = contentValues[idx++]
                    values.makeImageValues(image)
                    values.put("movie_id", imageList.id)
                    values.put("image_type", "poster")
                }
            }
            if (numBackdrops > 0) {
                for (image in imageList.backdrops) {
                    val values = contentValues[idx++]
                    values.makeImageValues(image)
                    values.put("movie_id", imageList.id)
                    values.put("image_type", "backdrop")
                }
            }
            mResolver.bulkInsert(mUris.movieImages(), contentValues)
        }

        fun ContentValues.makeImageValues(image: Image): ContentValues {
            val values = this
            values.put("height", image.height)
            values.put("width", image.width)
            values.put("file_path", image.filePath)
            values.put("vote_average", image.voteAverage)
            values.put("vote_count", image.voteCount)
            return values
        }

        fun setMovieAssociation(movie: String, year: String, id: Long) {
            val contentValues = ContentValues(2)
            contentValues.put("q", "$movie::$year")
            contentValues.put("movie_id", id)
            mResolver.insert(mUris.movieLookups(), contentValues)
        }

        fun getMovieAssociation(movie: String, year: String): Single<Long> {
            return Single.create { s ->
                mResolver.query(mUris.movieLookups(),
                        arrayOf("movie_id"), "q=?", arrayOf("$movie::$year"), null, null)?.use { c ->
                    if (c.moveToFirst()) {
                        s.onSuccess(c.getLong(0))
                    } else {
                        s.onError(NoSuchItemException())
                    }
                } ?: s.onError(VideoDatabaseMalfuction())
            }
        }

    }

}
