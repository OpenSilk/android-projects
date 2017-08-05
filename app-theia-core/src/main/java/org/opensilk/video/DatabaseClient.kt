package org.opensilk.video

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.database.ContentObserver
import android.database.Cursor
import android.media.browse.MediaBrowser
import android.net.Uri
import android.os.CancellationSignal
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
import org.opensilk.tmdb.api.model.Image
import org.opensilk.tmdb.api.model.ImageList
import org.opensilk.tmdb.api.model.Movie
import org.opensilk.tvdb.api.model.Series
import org.opensilk.tvdb.api.model.SeriesEpisode
import org.opensilk.tvdb.api.model.SeriesImageQuery
import org.opensilk.tvdb.api.model.Token
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

class VideoDatabaseMalfuction: Exception()

sealed class DatabaseChange
class UpnpUpdateIdChange: DatabaseChange()
class UpnpDeviceChange: DatabaseChange()
class UpnpFolderChange(val folderId: UpnpFolderId): DatabaseChange()
class UpnpVideoChange(val videoId: UpnpVideoId): DatabaseChange()

/**
 * Created by drew on 7/18/17.
 */
@Singleton
class DatabaseClient
@Inject constructor(
    @ForApplication context: Context,
    private val mUris: DatabaseUris,
    @Named("tvdb_banner_root") private val mTVDbBannerRoot: String
) : MediaProviderClient {

    internal var mResolver: ContentResolverGlue = DefaultContentResolverGlue(context.contentResolver)
    val uris = mUris

    private val mChangesSubject = BehaviorSubject.create<DatabaseChange>()

    val changesObservable: Observable<DatabaseChange>
        get() = mChangesSubject.hide()

    fun postChange(event: DatabaseChange) {
        mChangesSubject.onNext(event)
    }

    override fun getMediaItem(mediaRef: MediaRef): Single<MediaBrowser.MediaItem> {
        return getMediaMeta(mediaRef).map { it.toMediaItem() }
    }

    override fun getMediaMeta(mediaRef: MediaRef): Single<MediaMeta> {
        return when (mediaRef.kind) {
            UPNP_FOLDER -> getUpnpFolder(mediaRef.mediaId as UpnpFolderId)
            UPNP_VIDEO -> getUpnpVideo(mediaRef.mediaId as UpnpVideoId)
            UPNP_DEVICE -> getUpnpDevice(mediaRef.mediaId as UpnpDeviceId)
            else -> TODO()
        }
    }


    override fun getMediaArtworkUri(mediaRef: MediaRef): Single<Uri> {
        TODO("not implemented")
    }

    override fun siblingsOf(mediaRef: MediaRef): Observable<MediaMeta> {
        return when (mediaRef.kind) {
            UPNP_FOLDER -> {
                getUpnpFolder(mediaRef.mediaId as UpnpFolderId).flatMapObservable {
                    getUpnpVideos(newMediaRef(it.parentMediaId).mediaId as UpnpFolderId)
                }
            }
            UPNP_VIDEO -> {
                getUpnpVideo(mediaRef.mediaId as UpnpVideoId).flatMapObservable {
                    getUpnpVideos(newMediaRef(it.parentMediaId).mediaId as UpnpFolderId)
                }
            } else -> TODO()
        }
    }

    override fun getLastPlaybackPosition(mediaRef: MediaRef): Single<Long> {
        when (mediaRef.kind) {
            UPNP_VIDEO -> {
                return getUpnpVideo(mediaRef.mediaId as UpnpVideoId).flatMap { meta ->
                    Single.create<Long> { s ->
                        mResolver.query(mUris.playbackPosition(), arrayOf("last_position"),
                                "_display_name=?", arrayOf(meta.displayName), null, null)?.use { c ->
                            if (c.moveToFirst()) {
                                s.onSuccess(c.getLong(0))
                            } else {
                                s.onError(NoSuchItemException())
                            }
                        } ?: s.onError(VideoDatabaseMalfuction())
                    }
                }
            } else -> TODO()
        }
    }

    fun getLastPlaybackCompletion(mediaRef: MediaRef): Single<Int> {
        when (mediaRef.kind) {
            UPNP_VIDEO -> {
                return getUpnpVideo(mediaRef.mediaId as UpnpVideoId).flatMap { meta ->
                    Single.create<Int> { s ->
                        mResolver.query(mUris.playbackPosition(), arrayOf("last_completion"),
                                "_display_name=?", arrayOf(meta.displayName), null, null)?.use { c ->
                            if (c.moveToFirst()) {
                                s.onSuccess(c.getInt(0))
                            } else {
                                s.onError(NoSuchItemException())
                            }
                        } ?: s.onError(VideoDatabaseMalfuction())
                    }
                }
            } else -> TODO()
        }
    }

    override fun setLastPlaybackPosition(mediaRef: MediaRef, position: Long, duration: Long) {
        when (mediaRef.kind) {
            UPNP_VIDEO -> {
                getUpnpVideo(mediaRef.mediaId as UpnpVideoId)
                        .subscribeOn(AppSchedulers.diskIo)
                        .subscribeIgnoreError(Consumer { meta ->
                            val values = ContentValues()
                            values.put("_display_name", meta.displayName)
                            values.put("last_played", System.currentTimeMillis())
                            values.put("last_position", position)
                            values.put("last_completion", calculateCompletion(position, duration))
                            mResolver.insert(mUris.playbackPosition(), values)
                            postChange(UpnpVideoChange(mediaRef.mediaId as UpnpVideoId))
                        })
            } else -> TODO()
        }
    }


    fun getMediaOverview(mediaRef: MediaRef): Maybe<String> {
        return when (mediaRef.kind) {
            UPNP_VIDEO -> getUpnpVideoOverview(mediaRef.mediaId as UpnpVideoId)
            else -> TODO()
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
    fun getUpnpDevices(): Observable<MediaMeta> {
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

    fun setUpnpDeviceSystemUpdateId(deviceId: UpnpDeviceId, updateId: Long): Boolean {
        val values = ContentValues()
        values.put("update_id", updateId)
        return mResolver.update(mUris.upnpDevices(), values, "device_id=?", arrayOf(deviceId.deviceId)) != 0
    }

    fun incrementUpnpDeviceScanning(deviceId: UpnpDeviceId): Boolean {
        return mResolver.update(mUris.upnpDeviceIncrementScanning(),
                ContentValues(), "device_id=?", arrayOf(deviceId.deviceId)) != 0
    }

    fun decrementUpnpDeviceScanning(deviceId: UpnpDeviceId): Boolean {
        return mResolver.update(mUris.upnpDeviceDecrementScanning(),
                ContentValues(), "device_id=?", arrayOf(deviceId.deviceId)) != 0
    }

    fun getUpnpDeviceScanning(deviceId: UpnpDeviceId): Single<Long> {
        return Single.create { s ->
            mResolver.query(mUris.upnpDevices(), arrayOf("scanning"),
                    "device_id=?", arrayOf(deviceId.deviceId), null, null)?.use { c ->
                if (c.moveToFirst()) {
                    s.onSuccess(c.getLong(0))
                } else {
                    s.onError(NoSuchItemException())
                }
            } ?: s.onError(VideoDatabaseMalfuction())
        }
    }

    val upnpDeviceProjection = arrayOf("_id", "device_id", "mime_type", "title", //3
            "subtitle", "artwork_uri", "update_id") //6

    fun Cursor.toUpnpDeviceMediaMeta(): MediaMeta {
        val c = this
        val meta = MediaMeta()
        meta.rowId = c.getLong(0)
        meta.mediaId = MediaRef(UPNP_DEVICE, UpnpDeviceId(c.getString(1))).toJson()
        meta.mimeType = c.getString(2)
        meta.title = c.getString(3) ?: ""
        meta.subtitle = c.getString(4) ?: ""
        if (!c.isNull(5)) meta.artworkUri = Uri.parse(c.getString(5))
        meta.updateId = c.getLong(6)
        return meta
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
        cv.put("hidden", 0)
        return mResolver.insert(uris.upnpFolders(), cv) ?: Uri.EMPTY
    }

    /**
     * removes specified folder from database
     */
    fun removeUpnpFolder(rowid: Long): Boolean {
        return mResolver.delete(uris.upnpFolder(rowid), null, null) != 0
    }

    /**
     * retrieve direct decedents of parent folder that aren't hidden
     */
    fun getUpnpFolders(parentId: UpnpFolderId): Observable<MediaMeta> {
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
        cv.put("hidden", 0)
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
    fun getUpnpVideo(id: Long): Single<MediaMeta> {
        return Single.create { s ->
            mResolver.query(mUris.upnpVideo(id), upnpVideoProjection,
                    null, null, null, null)?.use { c ->
                if (c.moveToFirst()) {
                    s.onSuccess(c.toUpnpVideoMediaMeta())
                } else {
                    s.onError(NoSuchItemException())
                }
            } ?: s.onError(VideoDatabaseMalfuction())
        }
    }

    /**
     *
     */
    fun getUpnpVideo(videoId: UpnpVideoId): Single<MediaMeta> {
        return Single.create { s ->
            mResolver.query(uris.upnpVideos(), upnpVideoProjection,
                    "device_id=? AND item_id=?",
                    arrayOf(videoId.deviceId, videoId.itemId), null, null)?.use { c ->
                if (c.moveToFirst()) {
                    s.onSuccess(c.toUpnpVideoMediaMeta())
                } else {
                    s.onError(NoSuchItemException())
                }
            } ?: s.onError(VideoDatabaseMalfuction())
        }
    }

    fun getRecentUpnpVideos(): Observable<MediaMeta> {
        return Observable.create<MediaMeta> { s ->
            mResolver.query(mUris.upnpVideos(), upnpVideoProjection, null, null,
                    " v.date_added DESC LIMIT 20 ", s.cancellationSignal())?.use { c ->
                while (c.moveToNext()) {
                    s.onNext(c.toUpnpVideoMediaMeta())
                }
                s.onComplete()
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
            "s.poster as series_poster", "s.backdrop as series_backdrop", //16
            //movie columns
            "m._id as movie_id", "m._display_name as movie_title",  //18
            "m.poster_path as movie_poster", //19
            "m.backdrop_path as movie_backdrop") //20

    /**
     * helper to convert cursor to mediameta using above projection
     */
    fun Cursor.toUpnpVideoMediaMeta(): MediaMeta {
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
            //meta.extras.putLong("episode", c.getLong(9))
            //meta.extras.putLong("series", c.getLong(13))
            meta.title = c.getString(10)
            meta.subtitle = makeTvSubtitle(c.getString(14), c.getInt(11), c.getInt(12))
            if (!c.isNull(15)) {
                meta.artworkUri = makeTvBannerUri(c.getString(15))
            }
            if (!c.isNull(16)) {
                meta.backdropUri = makeTvBannerUri(c.getString(16))
            }
        } else if (!c.isNull(17)) {
            //meta.extras.putLong("movie", c.getLong(17))
            meta.title = c.getString(18)
            val movieBaseUrl = getMovieImageBaseUrl()
            if (!c.isNull(19) && movieBaseUrl != "") {
                meta.artworkUri = makeMoviePosterUri(movieBaseUrl, c.getString(19))
            }
            if (!c.isNull(20) && movieBaseUrl != "") {
                meta.backdropUri = makeMovieBackdropUri(movieBaseUrl, c.getString(20))
            }
        }
        return meta
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
                    if (!c.isNull(0)) {
                        s.onSuccess(c.getString(0))
                    } else if (!c.isNull(1)) {
                        s.onSuccess(c.getString(1))
                    } else {
                        s.onComplete()
                    }
                } else {
                    s.onError(NoSuchItemException())
                }
            } ?: s.onError(VideoDatabaseMalfuction())
        }
    }

    fun setUpnpVideoTvEpisodeId(videoId: UpnpVideoId, id: Long): Boolean {
        val cv = ContentValues()
        cv.put("episode_id", id)
        cv.put("movie_id", "")
        return mResolver.update(mUris.upnpVideos(), cv, "device_id=? AND item_id=?",
                arrayOf(videoId.deviceId, videoId.itemId)) != 0
    }

    fun setUpnpVideoMovieId(videoId: UpnpVideoId, id: Long): Boolean {
        val cv = ContentValues()
        cv.put("episode_id", "")
        cv.put("movie_id", id)
        return mResolver.update(mUris.upnpVideos(), cv, "device_id=? AND item_id=?",
                arrayOf(videoId.deviceId, videoId.itemId)) != 0
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

    fun getTvLastUpdate(): Single<Long> {
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
                    s.onError(NoSuchItemException())
                }
            } ?: s.onError(VideoDatabaseMalfuction())
        }
    }

    fun addTvSeries(series: Series, poster: SeriesImageQuery? = null,
                    backdrop: SeriesImageQuery? = null): Uri {
        val values = ContentValues(10)
        values.put("_id", series.id)
        values.put("_display_name", series.seriesName)
        values.put("overview", series.overview)
        values.put("first_aired", series.firstAired)
        values.put("banner", series.banner)
        values.put("poster", poster?.fileName)
        values.put("backdrop", backdrop?.fileName)
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

    fun addTvEpisodes(seriesId: Long, episodes: List<SeriesEpisode>) {
        val values = Array(episodes.size, { ContentValues() })
        for ((ii, episode) in episodes.withIndex()) {
            val cv = values[ii]
            cv.putTvEpisodeValues(episode)
            cv.put("series_id", seriesId)
        }
        mResolver.bulkInsert(mUris.tvEpisodes(), values)
    }

    fun ContentValues.putTvEpisodeValues(episode: SeriesEpisode) {
        val values = this
        values.put("_id", episode.id)
        values.put("_display_name", episode.episodeName)
        values.put("overview", episode.overview)
        values.put("first_aired", episode.firstAired)
        values.put("episode_number", episode.airedEpisodeNumber)
        values.put("season_number", episode.airedSeason)
    }

    fun getTvEpisodes(seriesId: Long): Observable<MediaMeta> {
        return Observable.create { s ->
            mResolver.query(mUris.tvEpisodes(), tvEpisodesProjection,
                    "series_id=?", arrayOf(seriesId.toString()),
                    null, s.cancellationSignal())?.use { c ->
                while (c.moveToNext()) {
                    s.onNext(c.toTvEpisodeMediaMeta())
                }
                s.onComplete()
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

    fun addTvImages(seriesId: Long, banners: List<SeriesImageQuery>) {
        val values = Array(banners.size, { ContentValues() })
        for ((ii, image) in banners.withIndex()) {
            val cv = values[ii]
            cv.putTvImageValues(image)
            cv.put("series_id", seriesId)
        }
        mResolver.bulkInsert(mUris.tvBanners(), values)
    }

    fun ContentValues.putTvImageValues(banner: SeriesImageQuery) {
        val values = this
        values.put("_id", banner.id)
        values.put("path", banner.fileName)
        values.put("type", banner.keyType)
        values.put("type2", banner.subKey)
        values.put("rating", banner.ratingsInfo.average)
        values.put("rating_count", banner.ratingsInfo.count)
        values.put("thumb_path", banner.thumbnail)
        values.put("resolution", banner.resolution)
    }

    fun getTvPosters(seriesId: Long): Observable<MediaMeta> {
        return Observable.create { s ->
            mResolver.query(mUris.tvBanners(), tvBannerProjection,
                    "series_id=? and type=?", arrayOf(seriesId.toString(), "poster"),
                    "rating DESC", s.cancellationSignal())?.use { c ->
                while (c.moveToNext()) {
                    s.onNext(c.toTvBannerMediaMeta())
                }
                s.onComplete()
            } ?: s.onError(VideoDatabaseMalfuction())
        }
    }

    fun getTvBackdrops(seriesId: Long): Observable<MediaMeta> {
        return Observable.create { s ->
            mResolver.query(mUris.tvBanners(), tvBannerProjection,
                    "series_id=? and type=?", arrayOf(seriesId.toString(), "fanart"),
                    "rating DESC", s.cancellationSignal())?.use { c ->
                while (c.moveToNext()) {
                    s.onNext(c.toTvBannerMediaMeta())
                }
                s.onComplete()
            } ?: s.onError(VideoDatabaseMalfuction())
        }
    }

    val tvBannerProjection = arrayOf("_id", "path", "resolution")

    fun Cursor.toTvBannerMediaMeta(): MediaMeta {
        val meta = MediaMeta()
        meta.mimeType = MIME_TYPE_JPEG
        meta.rowId = getLong(0)
        meta.artworkUri = makeTvBannerUri(getString(1))
        meta.resolution = getString(2)
        return meta
    }

    fun getTvSeriesAssociation(query: String): Single<Long> {
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

    fun setTvSeriesAssociation(query: String, series_id: Long) {
        val cv = ContentValues(2)
        cv.put("q", query)
        cv.put("series_id", series_id)
        val uri = mResolver.insert(mUris.tvLookups(), cv)
        //
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
                    val baseUrl = getMovieImageBaseUrl()
                    val meta = MediaMeta()
                    meta.mimeType = MIME_TYPE_MOVIE
                    meta.title = c.getString(0)
                    meta.overview = c.getString(1)
                    meta.releaseDate = c.getString(2)
                    if (!c.isNull(3) && baseUrl != "") {
                        meta.artworkUri = makeMoviePosterUri(baseUrl, c.getString(3))
                    }
                    if (!c.isNull(4) && baseUrl != "") {
                        meta.backdropUri = makeMovieBackdropUri(baseUrl, c.getString(4))
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
        values.put("file_path", image.filePath)
        values.put("resolution", "${image.width}x${image.height}")
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
