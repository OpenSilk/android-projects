package org.opensilk.media.database

import android.content.ContentResolver
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.OperationCanceledException
import android.provider.DocumentsContract
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.functions.Consumer
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import org.opensilk.media.*
import org.opensilk.reactivex2.cancellationSignal
import org.opensilk.reactivex2.subscribeIgnoreError
import timber.log.Timber
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
    private val mChangesScheduler = Schedulers.single()

    val changesObservable: Observable<DatabaseChange>
        get() = mChangesSubject.hide()

    internal fun postChange(event: DatabaseChange) {
        mChangesScheduler.scheduleDirect {
            when (event) {
                is FolderChange -> Timber.d("onFolderChange(${event.folderId}")
                is VideoChange -> Timber.d("onVideoChange(${event.videoId}")
                is DeviceChange -> Timber.d("onDeviceChange(${event.deviceId})")
                else -> Timber.d("onChange($event)")
            }
            mChangesSubject.onNext(event)
        }
    }

    fun postChangeFor(mediaId: MediaId) {
        postChange(when (mediaId) {
            is UpnpDeviceId -> UpnpDeviceChange(mediaId)
            is StorageDeviceId -> StorageDeviceChange(mediaId)
            is UpnpFolderId -> UpnpFolderChange(mediaId)
            is DocDirectoryId -> DocDirectoryChange(mediaId)
            is StorageFolderId -> StorageFolderChange(mediaId)
            is UpnpVideoId -> UpnpVideoChange(mediaId)
            is DocVideoId -> DocVideoChange(mediaId)
            is StorageVideoId -> StorageVideoChange(mediaId)
            else -> TODO("$mediaId")
        })
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

    private fun <T> doGet(uri: Uri, projection: Array<out String>?, selection: String?,
                          selectionArgs: Array<out String>?,
                          converter: (c: Cursor) -> T): Maybe<T> {
        return Maybe.create { s ->
            try {
                mResolver.query(uri, projection, selection, selectionArgs,
                        null, s.cancellationSignal())?.use { c ->
                    if (c.moveToFirst()) {
                        s.onSuccess(converter(c))
                    } else {
                        s.onComplete()
                    }
                } ?: s.onError(VideoDatabaseMalfuction())
            } catch (e: OperationCanceledException) {
                //pass
            }
        }
    }

    fun getMediaRef(mediaId: MediaId): Maybe<out MediaRef> = when (mediaId) {
        is UpnpDeviceId -> getUpnpDevice(mediaId)
        is StorageDeviceId -> getStorageDevice(mediaId)

        is FolderId -> getFolderRef(mediaId)
        is VideoId -> getVideoRef(mediaId)
        else -> TODO()
    }

    fun getVideoRef(videoId: VideoId): Maybe<out VideoRef> = when (videoId) {
        is UpnpVideoId -> getUpnpVideo(videoId)
        is DocVideoId -> getDocVideo(videoId)
        is StorageVideoId -> getStorageVideo(videoId)
        else -> TODO()
    }

    fun getFolderRef(folderId: FolderId): Maybe<out FolderRef> = when (folderId) {
        is UpnpFolderId -> getUpnpFolder(folderId)
        is DocDirectoryId -> getDocDirectory(folderId)
        is StorageFolderId -> getStorageFolder(folderId)
        else -> TODO()
    }

    fun getVideoOverview(mediaId: VideoId): Maybe<String> = when (mediaId) {
        is UpnpVideoId -> getUpnpVideoOverview(mediaId)
        is DocVideoId -> getDocVideoOverview(mediaId)
        is StorageVideoId -> getStorageVideoOverview(mediaId)
        else -> TODO()
    }

    fun getRecentlyPlayedVideos(): Observable<VideoRef> {
        return Observable.merge(
                getRecentlyPlayedUpnpVideos(),
                getRecentlyPlayedDocVideos(),
                getRecentlyPlayedStorageVideos()
        ).sorted({ left, right ->
            //decending
            ((right.resumeInfo?.lastPlayed ?: 0L) - (left.resumeInfo?.lastPlayed ?: 0L)).toInt()
        }).take(10)
    }

    fun setVideoTvEpisodeId(mediaId: VideoId, episodeId: TvEpisodeId) {
        when (mediaId) {
            is UpnpVideoId -> {
                setUpnpVideoTvEpisodeId(mediaId, episodeId)
                postChangeFor(mediaId)
            }
            is DocVideoId -> {
                setDocVideoTvEpisodeId(mediaId, episodeId)
                postChangeFor(mediaId)
            }
            is StorageVideoId -> {
                setStorageVideoTvEpisodeId(mediaId, episodeId)
                postChangeFor(mediaId)
            }
            else -> TODO()
        }
    }

    fun setVideoMovieId(mediaId: VideoId, movieId: MovieId) {
        when (mediaId) {
            is UpnpVideoId -> {
                setUpnpVideoMovieId(mediaId, movieId)
                postChangeFor(mediaId)
            }
            is DocVideoId -> {
                setDocVideoMovieId(mediaId, movieId)
                postChangeFor(mediaId)
            }
            is StorageVideoId -> {
                setStorageVideoMovieId(mediaId, movieId)
                postChangeFor(mediaId)
            }
            else -> TODO()
        }
    }

    /**
     * Used by [PlaybackSession]
     */
    fun playableSiblingVideos(videoId: VideoId): Observable<out VideoRef> = when (videoId) {
        is UpnpVideoId -> {
            getUpnpVideosUnder(UpnpFolderId(deviceId = videoId.deviceId,
                    parentId = "", containerId = videoId.parentId))
        }
        is DocVideoId -> {
            getDocVideosUnder(DocDirectoryId(treeUri = videoId.treeUri,
                    documentId = videoId.parentId, parentId = ""))
        }
        is StorageVideoId -> {
            getStorageVideosUnder(StorageFolderId(uuid = videoId.uuid,
                    path = videoId.parent, parent = ""))
        }
        is IntentDataVideoId -> {
            Observable.just(IntentDataVideoRef(
                    id = videoId,
                    meta = IntentDataVideoMeta(
                            title = "Video",
                            mimeType = "video/*",
                            mediaUri = videoId.uri)))
        }
        else -> TODO()
    }

    /**
     * Used by [PlaybackSession]
     */
    fun setLastPlaybackPosition(mediaId: MediaId, position: Long, duration: Long) {
        val values = positionContentVals(position, duration)
        when (mediaId) {
            is UpnpVideoId -> {
                setUpnpVideoLastPlayed(videoId = mediaId, lastPlayed = values.getAsLong("last_played"))
                setVideoLastPlaybackPosition(mediaId, values)
            }
            is DocVideoId -> {
                setDocVideoLastPlayed(documentId = mediaId, lastPlayed = values.getAsLong("last_played"))
                setVideoLastPlaybackPosition(mediaId, values)
            }
            is StorageVideoId -> {
                setStorageVideoLastPlayed(videoId = mediaId, lastPlayed = values.getAsLong("last_played"))
                setVideoLastPlaybackPosition(mediaId, values)
            }
            is IntentDataVideoId -> {
                Timber.i("Ignoring last playback position on ephemeral video $mediaId")
            }
            else -> TODO()
        }
    }

    private fun setVideoLastPlaybackPosition(videoId: VideoId, values: ContentValues) {
        getVideoRef(videoId).subscribeIgnoreError(Consumer { meta ->
            values.put("_display_name", meta.meta.title)
            mResolver.insert(mUris.playbackPosition(), values)
            postChangeFor(videoId)
        })
    }

    fun hideChildrenOf(mediaId: MediaId) {
        when (mediaId) {
            is UpnpContainerId -> hideChildrenOf(mediaId)
            is DocDirectoryId -> hideChildrenOf(mediaId)
            is StorageContainerId -> hideChildrenOf(mediaId)
            else -> TODO()
        }
    }

    /*
     * START PINS
     */

    fun getPinnedItems(): Observable<MediaRef> =
            doQuery(mUris.pins(), arrayOf("media_id"),
                    null, null, null,
                    { c -> c.getString(0).toMediaId() }
            ).flatMapMaybe { mediaId -> getMediaRef(mediaId) }

    fun checkPinned(mediaId: MediaId): Single<Boolean> =
            Single.fromCallable {
                mResolver.query(mUris.pins(), arrayOf("media_id"), "media_id=?",
                        arrayOf(mediaId.json), null, null)?.use { c ->
                    return@use c.count > 0
                } ?: false
            }

    fun pinItem(mediaId: MediaId): Boolean =
            mResolver.insert(mUris.pins(), contentValues("media_id", mediaId.json)).apply {
                postChangeFor(mediaId)
            } == URI_SUCCESS

    fun unpinItem(mediaId: MediaId): Boolean =
            mResolver.delete(mUris.pins(), "media_id=?", arrayOf(mediaId.json)).apply {
                postChangeFor(mediaId)
            } != 0

    /*
     * START UPNP
     */

    /**
     * Add a meta item describing a upnp device with a content directory service to the database
     * item should be created with Device.toMediaMeta
     */
    fun addUpnpDevice(upnpDevice: UpnpDeviceRef) = mResolver.insert(mUris.upnpDevice(),
            upnpDevice.contentValues()).apply {
        postChangeFor(upnpDevice.id)
    } == URI_SUCCESS

    /**
     * marks the upnp device with giving identity as unavailable
     */
    fun hideUpnpDevice(identity: String): Boolean = mResolver.update(mUris.upnpDevice(),
            contentValues("available", 0), "device_id=?", arrayOf(identity)).apply {
        postChangeFor(UpnpDeviceId(identity))
    } != 0

    fun hideAllUpnpDevices(): Boolean = mResolver.update(mUris.upnpDevice(),
            contentValues("available", 0), null, null) != 0

    /**
     * retrieves all the upnp devices marked as available
     */
    fun getAvailableUpnpDevices(): Observable<UpnpDeviceRef> =
            doQuery(mUris.upnpDevice(), upnpDeviceProjection,
                    "available=1", null, "title",
                    {c -> c.toUpnpDeviceRef()})

    fun getUpnpDevice(deviceId: UpnpDeviceId): Maybe<UpnpDeviceRef> {
        return doGet(mUris.upnpDevice(), upnpDeviceProjection,
                "device_id=?", arrayOf(deviceId.deviceId), { c -> c.toUpnpDeviceRef()})
    }

    fun setUpnpDeviceSystemUpdateId(deviceId: UpnpDeviceId, updateId: Long): Boolean {
        return mResolver.update(mUris.upnpDevice(), contentValues("update_id", updateId),
                "device_id=?", arrayOf(deviceId.deviceId)) != 0
    }

    fun getUpnpDeviceSystemUpdateId(deviceId: UpnpDeviceId): Maybe<Long> {
        return doGet(mUris.upnpDevice(), arrayOf("update_id"), "device_id=?",
                arrayOf(deviceId.deviceId), { c ->c.getLong(0) })
    }

    /**
     * add a upnp folder to the database, item should be created with Container.toMediaMeta
     */
    fun addUpnpFolder(folderRef: UpnpFolderRef) =
            mResolver.insert(mUris.upnpFolder(), folderRef.contentValues()) == URI_SUCCESS

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
        return doGet(mUris.upnpFolder(), upnpFolderProjection,
                "device_id=? AND parent_id=? AND folder_id=?",
                arrayOf(folderId.deviceId, folderId.parentId, folderId.containerId),
                {c -> c.toUpnpFolderRef() })
    }

    /**
     * Adds upnp video to database, item should be created with VideoItem.toMediaMeta
     */
    fun addUpnpVideo(videoRef: UpnpVideoRef) =
            mResolver.insert(mUris.upnpVideo(), videoRef.contentValues()) == URI_SUCCESS

    /**
     * retrieve upnp videos, direct decedents of parent
     */
    fun getUpnpVideosUnder(parentId: UpnpContainerId): Observable<UpnpVideoRef> {
        return doQuery(mUris.upnpVideo(), upnpVideoProjection,
                "v.device_id=? AND v.parent_id=? AND v.hidden=0",
                arrayOf(parentId.deviceId, parentId.containerId),
                "v._display_name", { c ->c.toUpnpVideoMediaMeta(mApiHelper) })
    }

    /**
     * retrieve specified upnp video
     */
    fun getUpnpVideo(videoId: UpnpVideoId): Maybe<UpnpVideoRef> {
        return doGet(mUris.upnpVideo(), upnpVideoProjection,
                "v.device_id=? AND v.parent_id=? AND v.item_id=?",
                arrayOf(videoId.deviceId, videoId.parentId, videoId.itemId),
                {c -> c.toUpnpVideoMediaMeta(mApiHelper) })
    }

    fun setUpnpVideoLastPlayed(videoId: UpnpVideoId, lastPlayed: Long): Boolean =
            mResolver.update(mUris.upnpVideo(), contentValues("last_played", lastPlayed),
                    "device_id=? AND parent_id=? AND item_id=?",
                    arrayOf(videoId.deviceId, videoId.parentId, videoId.itemId)) != 0

    fun getRecentlyPlayedUpnpVideos(): Observable<UpnpVideoRef> {
        return doQuery(mUris.upnpVideo(), upnpVideoProjection, "v.last_played > 0", null,
                " v.last_played DESC LIMIT 10 ", { c -> c.toUpnpVideoMediaMeta(mApiHelper) })
    }

    fun getUpnpVideoOverview(videoId: UpnpVideoId): Maybe<String> {
        return Maybe.create { s ->
            mResolver.query(mUris.upnpVideo(),
                    arrayOf("e.overview as episode_overview",
                            "m.overview as movie_overview"),
                    "v.device_id=? AND v.parent_id=? AND v.item_id=?",
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

    fun addUpnpMusicTrack(track: UpnpMusicTrackRef): Boolean =
            mResolver.insert(mUris.upnpMusicTrack(), track.contentValues()) == URI_SUCCESS

    /**
     * retrieve upnp videos, direct decedents of parent
     */
    fun getUpnpMusicTracksUnder(parentId: UpnpContainerId): Observable<UpnpMusicTrackRef> {
        return doQuery(mUris.upnpMusicTrack(), upnpMusicTrackProjection,
                "t.device_id=? AND t.parent_id=? AND t.hidden=0",
                arrayOf(parentId.deviceId, parentId.containerId),
                "t._display_name", { c ->c.toUpnpMusicTrackMediaMeta(mApiHelper) })
    }

    /**
     * retrieve specified upnp video
     */
    fun getUpnpMusicTrack(trackId: UpnpMusicTrackId): Maybe<UpnpMusicTrackRef> {
        return doGet(mUris.upnpMusicTrack(), upnpMusicTrackProjection,
                "t.device_id=? AND t.parent_id=? AND t.item_id=?",
                arrayOf(trackId.deviceId, trackId.parentId, trackId.itemId),
                {c -> c.toUpnpMusicTrackMediaMeta(mApiHelper) })
    }

    /**
     * Marks hidden column on upnp folders and videos with specified parent
     */
    fun hideChildrenOf(parentId: UpnpContainerId) {
        val cv = contentValues("hidden", 1)
        mResolver.update(mUris.upnpFolder(), cv, "device_id=? AND parent_id=?",
                arrayOf(parentId.deviceId, parentId.containerId))
        mResolver.update(mUris.upnpVideo(), cv, "device_id=? AND parent_id=?",
                arrayOf(parentId.deviceId, parentId.containerId))
        mResolver.update(mUris.upnpMusicTrack(), cv, "device_id=? AND parent_id=?",
                arrayOf(parentId.deviceId, parentId.containerId))
    }

    /*
     * END UPNP
     */

    /*
     * START DOCUMENT
     */

    fun addDocDirectory(documentRef: DocDirectoryRef) =
            mResolver.insert(mUris.documentDirectory(), documentRef.contentValues()) == URI_SUCCESS

    fun getDocDirectoryUnder(documentId: DocDirectoryId): Observable<DocDirectoryRef> {
        return doQuery(mUris.documentDirectory(), directoryDocumentProjection,
                "tree_uri=? AND parent_id=? AND hidden=0",
                arrayOf(documentId.treeUri.toString(), documentId.documentId),
                "_display_name", { c ->c.toDirectoryDocument() })
    }

    fun getDocDirectory(documentId: DocDirectoryId): Maybe<DocDirectoryRef> {
        return doGet(mUris.documentDirectory(), directoryDocumentProjection,
                "tree_uri=? AND document_id=? AND parent_id=?",
                arrayOf(documentId.treeUri.toString(), documentId.documentId, documentId.parentId),
                { c -> c.toDirectoryDocument() })
    }

    fun addDocVideo(documentRef: DocVideoRef) =
            mResolver.insert(mUris.documentVideo(), documentRef.contentValues()) == URI_SUCCESS

    fun getDocVideosUnder(documentId: DocDirectoryId): Observable<DocVideoRef> {
        return doQuery(mUris.documentVideo(), videoDocumentProjection,
                "tree_uri=? AND parent_id=? AND hidden=0",
                arrayOf(documentId.treeUri.toString(), documentId.documentId),
                "v._display_name", { c -> c.toVideoDocumentRef(mApiHelper) })
    }

    fun getDocVideo(documentId: DocVideoId): Maybe<DocVideoRef> {
        return doGet(mUris.documentVideo(), videoDocumentProjection,
                "tree_uri=? AND document_id=? AND parent_id=?",
                arrayOf(documentId.treeUri.toString(), documentId.documentId, documentId.parentId),
                {c -> c.toVideoDocumentRef(mApiHelper) })
    }

    fun setDocVideoLastPlayed(documentId: DocVideoId, lastPlayed: Long): Boolean =
            mResolver.update(mUris.documentVideo(), contentValues("last_played", lastPlayed),
                    "tree_uri=? AND document_id=? AND parent_id=?", arrayOf(documentId.treeUri.toString(),
                    documentId.documentId, documentId.parentId)) != 0

    fun getRecentlyPlayedDocVideos(): Observable<DocVideoRef> {
        return doQuery(mUris.documentVideo(), videoDocumentProjection, "v.last_played > 0", null,
                " v.last_played DESC LIMIT 10 ", { c -> c.toVideoDocumentRef(mApiHelper) })
    }

    fun getDocVideoOverview(documentId: DocVideoId): Maybe<String> {
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

    fun setDocVideoTvEpisodeId(documentId: DocVideoId, tvEpisodeId: TvEpisodeId): Boolean {
        val values = ContentValues()
        values.put("episode_id", tvEpisodeId.episodeId)
        values.put("movie_id", "")
        return mResolver.update(mUris.documentVideo(), values, "tree_uri=? AND document_id=? AND parent_id=?",
                arrayOf(documentId.treeUri.toString(), documentId.documentId, documentId.parentId)) != 0
    }

    fun setDocVideoMovieId(documentId: DocVideoId, movieId: MovieId): Boolean {
        val values = ContentValues()
        values.put("episode_id", "")
        values.put("movie_id", movieId.movieId)
        return mResolver.update(mUris.documentVideo(), values, "tree_uri=? AND document_id=? AND parent_id=?",
                arrayOf(documentId.treeUri.toString(), documentId.documentId, documentId.parentId)) != 0
    }

    fun addDocMusicTrack(documentRef: DocMusicTrackRef) =
            mResolver.insert(mUris.documentMusicTrack(), documentRef.contentValues()) == URI_SUCCESS

    fun getDocMusicTracksUnder(documentId: DocDirectoryId): Observable<DocMusicTrackRef> {
        return doQuery(mUris.documentMusicTrack(), docMusicTrackProjection,
                "t.tree_uri=? AND t.parent_id=? AND t.hidden=0",
                arrayOf(documentId.treeUri.toString(), documentId.documentId),
                "t._display_name", { c -> c.toDocMusicTrackRef(mApiHelper) })
    }

    fun getDocMusicTrack(documentId: DocMusicTrackId): Maybe<DocMusicTrackRef> {
        return doGet(mUris.documentMusicTrack(), docMusicTrackProjection,
                "t.tree_uri=? AND t.document_id=? AND t.parent_id=?",
                arrayOf(documentId.treeUri.toString(), documentId.documentId, documentId.parentId),
                {c -> c.toDocMusicTrackRef(mApiHelper) })
    }

    fun hideChildrenOf(documentId: DocDirectoryId): Boolean {
        val values = contentValues("hidden", 1)
        var num = 0
        num += mResolver.update(mUris.documentDirectory(), values,
                "tree_uri=? AND document_id=? AND parent_id=?",
                arrayOf(documentId.treeUri.toString(), documentId.documentId, documentId.parentId))
        num += mResolver.update(mUris.documentVideo(), values,
                "tree_uri=? AND document_id=? AND parent_id=?",
                arrayOf(documentId.treeUri.toString(), documentId.documentId, documentId.parentId))
        num += mResolver.update(mUris.documentMusicTrack(), values,
                "tree_uri=? AND document_id=? AND parent_id=?",
                arrayOf(documentId.treeUri.toString(), documentId.documentId, documentId.parentId))
        return num != 0
    }

    /*
     * END DOCUMENTS
     */

    /*
     * START STORAGE
     */

    fun addStorageDevice(deviceRef: StorageDeviceRef) =
            mResolver.insert(mUris.storageDevice(), deviceRef.contentValues()).apply {
                postChangeFor(deviceRef.id)
            } == URI_SUCCESS

    fun hideAllStorageDevices(): Boolean = mResolver.update(mUris.storageDevice(),
            contentValues("hidden", 1), null, null) != 0

    fun getAvailableStorageDevices(): Observable<StorageDeviceRef> =
            doQuery(mUris.storageDevice(), storageDeviceProjection,
                "hidden=0", null, "_display_name",
                { c -> c.toStorageDevice() })

    fun getStorageDevice(deviceId: StorageDeviceId): Maybe<StorageDeviceRef> =
            doGet(mUris.storageDevice(), storageDeviceProjection,
                    "uuid=?",
                    arrayOf(deviceId.uuid),
                    { c -> c.toStorageDevice() })

    fun addStorageFolder(folderRef: StorageFolderRef) =
            mResolver.insert(mUris.storageFolder(), folderRef.contentValues()) == URI_SUCCESS

    fun getStorageFoldersUnder(containerId: StorageContainerId): Observable<StorageFolderRef> =
            doQuery(mUris.storageFolder(), storageFolderProjection,
                "f.parent_path=? AND device_uuid=? AND f.hidden=0",
                arrayOf(containerId.path, containerId.uuid),
                "f._display_name", { c ->c.toStorageFolder() })

    fun getStorageFolder(folderId: StorageFolderId): Maybe<StorageFolderRef> =
            doGet(mUris.storageFolder(), storageFolderProjection,
                "f.path=? AND device_uuid=?",
                arrayOf(folderId.path, folderId.uuid),
                { c -> c.toStorageFolder() })

    fun addStorageVideo(videoRef: StorageVideoRef) =
            mResolver.insert(mUris.storageVideo(), videoRef.contentValues()) == URI_SUCCESS

    fun getStorageVideosUnder(containerId: StorageContainerId): Observable<StorageVideoRef> =
            doQuery(mUris.storageVideo(), storageVideoProjection,
                "v.parent_path=? AND v.device_uuid=? AND v.hidden=0",
                arrayOf(containerId.path, containerId.uuid),
                "v._display_name",
                { c -> c.toStorageVideo(mApiHelper) })

    fun getStorageVideo(videoId: StorageVideoId): Maybe<StorageVideoRef> =
            doGet(mUris.storageVideo(), storageVideoProjection,
                "v.path=? AND v.device_uuid=?",
                arrayOf(videoId.path, videoId.uuid),
                {c -> c.toStorageVideo(mApiHelper) })

    fun setStorageVideoLastPlayed(videoId: StorageVideoId, lastPlayed: Long): Boolean =
            mResolver.update(mUris.storageVideo(), contentValues("last_played", lastPlayed),
                    "path=? AND device_uuid=?", arrayOf(videoId.path, videoId.uuid)) != 0

    fun getRecentlyPlayedStorageVideos(): Observable<StorageVideoRef> =
            doQuery(mUris.storageVideo(), storageVideoProjection, "v.last_played > 0", null,
                " v.last_played DESC LIMIT 10 ", { c -> c.toStorageVideo(mApiHelper) })

    fun getStorageVideoOverview(videoId: StorageVideoId): Maybe<String> {
        return Maybe.create { s ->
            mResolver.query(mUris.storageVideo(),
                    arrayOf("e.overview as episode_overview",
                            "m.overview as movie_overview"),
                    "v.path=? AND v.device_uuid=?",
                    arrayOf(videoId.path, videoId.uuid), null, null)?.use { c ->
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

    fun setStorageVideoTvEpisodeId(videoId: StorageVideoId, tvEpisodeId: TvEpisodeId): Boolean {
        val values = ContentValues()
        values.put("episode_id", tvEpisodeId.episodeId)
        values.put("movie_id", "")
        return mResolver.update(mUris.storageVideo(), values, "path=? AND device_uuid=?",
                arrayOf(videoId.path, videoId.uuid)) != 0
    }

    fun setStorageVideoMovieId(videoId: StorageVideoId, movieId: MovieId): Boolean {
        val values = ContentValues()
        values.put("episode_id", "")
        values.put("movie_id", movieId.movieId)
        return mResolver.update(mUris.storageVideo(), values, "path=? AND device_uuid=?",
                arrayOf(videoId.path, videoId.uuid)) != 0
    }

    fun addStorageMusicTrack(videoRef: StorageMusicTrackRef) =
            mResolver.insert(mUris.storageMusicTrack(), videoRef.contentValues()) == URI_SUCCESS

    fun getStorageMusicTracksUnder(containerId: StorageContainerId): Observable<StorageMusicTrackRef> =
            doQuery(mUris.storageMusicTrack(), storageMusicTrackProjection,
                    "t.parent_path=? AND t.device_uuid=? AND t.hidden=0",
                    arrayOf(containerId.path, containerId.uuid),
                    "t._display_name",
                    { c -> c.toStorageMusicTrack(mApiHelper) })

    fun getStorageMusicTrack(trackId: StorageMusicTrackId): Maybe<StorageMusicTrackRef> =
            doGet(mUris.storageMusicTrack(), storageMusicTrackProjection,
                    "t.path=? AND t.device_uuid=?",
                    arrayOf(trackId.path, trackId.uuid),
                    {c -> c.toStorageMusicTrack(mApiHelper) })

    fun hideChildrenOf(containerId: StorageContainerId): Boolean {
        val values = contentValues("hidden", 1)
        var num = 0
        num += mResolver.update(mUris.storageFolder(), values,
                "parent_path=? AND device_uuid=?",
                arrayOf(containerId.path, containerId.uuid))
        num += mResolver.update(mUris.storageVideo(), values,
                "parent_path=? AND device_uuid=?",
                arrayOf(containerId.path, containerId.uuid))
        return num != 0
    }

    /*
     * END STORAGE
     */

    /*
     * START TV
     */

    fun addTvSeries(series: TvSeriesRef) =
            mResolver.insert(mUris.tvSeries(), series.contentValues()) == URI_SUCCESS

    fun getTvSeries(seriesId: TvSeriesId): Maybe<TvSeriesRef> =
            doGet(mUris.tvSeries(), tvSeriesProjection,
                "_id=?", arrayOf(seriesId.seriesId.toString()),
                { c -> c.toTvSeries() })

    fun addTvEpisodes(episodes: List<TvEpisodeRef>): Int {
        val values = Array(episodes.size, { idx ->
            episodes[idx].contentValues()
        })
        return mResolver.bulkInsert(mUris.tvEpisode(), values)
    }

    fun getTvEpisodesForTvSeries(seriesId: TvSeriesId): Observable<TvEpisodeRef> =
            doQuery(mUris.tvEpisode(), tvEpisodesProjection,
                "series_id=?", arrayOf(seriesId.seriesId.toString()),
                null, { c ->c.toTvEpisodeMediaMeta() })

    fun getTvEpisode(episodeId: TvEpisodeId): Maybe<TvEpisodeRef> =
            doGet(mUris.tvEpisode(), tvEpisodesProjection,
                "_id=?", arrayOf(episodeId.episodeId.toString()),
                { c -> c.toTvEpisodeMediaMeta() })

    fun addTvImages(banners: List<TvImageRef>): Int {
        val values = Array(banners.size, { idx ->
            banners[idx].contentValues()
        })
        return mResolver.bulkInsert(mUris.tvImage(), values)
    }

    fun getTvPosters(seriesId: TvSeriesId): Observable<TvImageRef> =
            doQuery(mUris.tvImage(), tvBannerProjection,
                "series_id=? and type=?", arrayOf(seriesId.seriesId.toString(), "poster"),
                "rating DESC", { c ->c.toTvBannerMediaMeta() })

    fun getTvBackdrops(seriesId: TvSeriesId): Observable<TvImageRef> =
            doQuery(mUris.tvImage(), tvBannerProjection,
                "series_id=? and type=?", arrayOf(seriesId.seriesId.toString(), "fanart"),
                "rating DESC", { c ->c.toTvBannerMediaMeta() })

    /*
     * END TV
     */

    /*
     * START MOVIE
     */

    fun addMovie(movie: MovieRef) =
            mResolver.insert(mUris.movie(), movie.contentValues()) == URI_SUCCESS

    fun getMovie(movieId: MovieId): Maybe<MovieRef> =
            doGet(mUris.movie(), movieProjection,
                "_id=?", arrayOf(movieId.movieId.toString()),
                { c -> c.toMovieRef() })

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
        "v.device_id", "v.parent_id", "v.item_id", "d.available", //3
        "v._display_name", "v.mime_type", "v.media_uri", "v.duration", "v.file_size", //8
        //episode columns
        "e._id", "e._display_name", //10
        "e.episode_number", "e.season_number", //12
        //series columns
        "s._id", "s._display_name", //14
        "s.poster", "s.backdrop", //16
        //movie columns
        "m._id", "m._display_name",  //18
        "m.poster_path", "m.backdrop_path", //20
        //more episode columns
        "e.poster", "e.backdrop", //22
        //completion info
        "p.last_position", "p.last_completion", "p.last_played" //25
)

/**
 * helper to convert cursor to mediameta using above projection
 */
fun Cursor.toUpnpVideoMediaMeta(mApiHelper: ApiHelper): UpnpVideoRef {
    val c = this
    val mediaId = UpnpVideoId(c.getString(0), c.getString(1), c.getString(2))
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
            backdropUri = mApiHelper.movieImageBackdropUri(c.getString(20))
        }
    }
    var resume: VideoResumeInfo? = null
    if (!isNull(23) && !isNull(24) && !isNull(25)) {
        resume = VideoResumeInfo(lastPosition = getLong(23),
                lastCompletion = getInt(24), lastPlayed = getLong(25))
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
            ),
            resumeInfo = resume
    )
}

fun UpnpMusicTrackRef.contentValues(): ContentValues {
    val values = ContentValues()
    values.put("device_id", id.deviceId)
    values.put("parent_id", id.parentId)
    values.put("item_id", id.itemId)
    values.put("_display_name", meta.originalTitle.elseIfBlank(meta.title))
    values.put("genre", meta.genre)
    values.put("artist", meta.artist)
    values.put("album", meta.album)
    values.put("date", meta.date)
    values.put("track_num", meta.trackNum)
    values.put("mime_type", meta.mimeType)
    values.put("media_uri", meta.mediaUri.toString())
    values.put("duration", meta.duration)
    values.put("bitrate", meta.bitrate)
    values.put("file_size", meta.size)
    values.put("n_channels", meta.nrAudioChan)
    values.put("s_freq", meta.sampleFreq)
    if (meta.originalArtworkUri != Uri.EMPTY) {
        values.put("artwork_uri", meta.originalArtworkUri.toString())
    }
    values.put("date_added", System.currentTimeMillis())
    values.put("hidden", 0)
    return values
}

val upnpMusicTrackProjection = arrayOf(
        //upnp_music_track columns
        "t.device_id", "t.parent_id", "t.item_id", //2
        "t._display_name", "t.genre", "t.artist", "t.album", //6
        "t.date", "t.track_num", "t.mime_type", "t.media_uri", //10
        "t.duration", "t.bitrate", "t.file_size", "n_channels", //14
        "t.s_freq", "t.artwork_uri" //16
)

fun Cursor.toUpnpMusicTrackMediaMeta(helper: ApiHelper): UpnpMusicTrackRef {
    val device_id = getString(0)
    val parent_id = getString(1)
    val item_id = getString(2)
    val title = getString(3)
    val genre = getString(4)
    val artist = getString(5)
    val album = getString(6)
    val date = getString(7)
    val track_num = getInt(8)
    val mime = getString(9)
    val media_uri = Uri.parse(getString(10))
    val duration = getLong(11)
    val bitrate = getLong(12)
    val size = getLong(13)
    val n_chan = getInt(14)
    val s_freq = getLong(15)
    val artwork_uri = if (!isNull(16)) Uri.parse(getString(16)) else Uri.EMPTY
    return UpnpMusicTrackRef(
            id = UpnpMusicTrackId(
                    deviceId = device_id,
                    parentId = parent_id,
                    itemId = item_id
            ),
            meta = UpnpMusicTrackMeta(
                    title = title,
                    genre = genre,
                    artist = artist,
                    album = album,
                    date = date,
                    trackNum = track_num,
                    mimeType = mime,
                    mediaUri = media_uri,
                    duration = duration,
                    bitrate = bitrate,
                    size = size,
                    nrAudioChan = n_chan,
                    sampleFreq = s_freq,
                    artworkUri = artwork_uri
            )
    )
}

fun DocDirectoryRef.contentValues(): ContentValues {
    val values = ContentValues()
    values.put("authority", id.treeUri.authority)
    values.put("tree_uri", id.treeUri.toString())
    values.put("document_id", id.documentId)
    values.put("parent_id", id.parentId)
    values.put("_display_name", meta.title)
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

fun Cursor.toDirectoryDocument(): DocDirectoryRef {
    val id = DocDirectoryId(
            treeUri = Uri.parse(getString(0)),
            documentId = getString(1),
            parentId = getString(2))
    val mimetype = getString(3)
    val displayname = getString(4)
    val flags = if (!isNull(5)) getLong(5) else 0L
    val lastmod = if (!isNull(6)) getLong(6) else 0L
    return DocDirectoryRef(
            id = id,
            meta = DocDirectoryMeta(
                    title = displayname,
                    mimeType = mimetype,
                    flags = flags,
                    lastMod = lastmod
            )
    )
}

fun DocVideoRef.contentValues(): ContentValues {
    val values = ContentValues()
    val documentRef = this
    values.put("authority", documentRef.id.treeUri.authority)
    values.put("tree_uri", documentRef.id.treeUri.toString())
    values.put("document_id", documentRef.id.documentId)
    values.put("parent_id", documentRef.id.parentId)
    values.put("_display_name", documentRef.meta.originalTitle.elseIfBlank(documentRef.meta.title))
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
        "tree_uri", "document_id", "parent_id", "parent_id", //3
        "v._display_name", "v.mime_type", "v._size", "v.flags", "v.last_modified", //8
        //episode columns
        "e._id", "e._display_name", //10
        "e.episode_number", "e.season_number", //12
        //series columns
        "s._id", "s._display_name", //14
        "s.poster", "s.backdrop", //16
        //movie columns
        "m._id", "m._display_name",  //18
        "m.poster_path", "m.backdrop_path", //20
        //more episode columns
        "e.poster", "e.backdrop", //22
        //completion info
        "p.last_position", "p.last_completion", "p.last_played" //25
)

/**
 * helper to convert cursor to mediameta using above projection
 */
fun Cursor.toVideoDocumentRef(mApiHelper: ApiHelper): DocVideoRef {
    val treeUri = Uri.parse(getString(0))
    val docId = getString(1)
    val parentId = getString(2)
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
    var resume: VideoResumeInfo? = null
    if (!isNull(23) && !isNull(24) && !isNull(25)) {
        resume = VideoResumeInfo(lastPosition = getLong(23),
                lastCompletion = getInt(24), lastPlayed = getLong(25))
    }
    val docid = DocVideoId(
            treeUri = treeUri,
            documentId = docId,
            parentId = parentId)
    return DocVideoRef(
            id = docid,
            tvEpisodeId =  episodeId,
            movieId = movieId,
            meta = DocVideoMeta(
                    title = title,
                    subtitle = subtitle,
                    artworkUri = artworkUri,
                    backdropUri = backdropUri,
                    originalTitle = displayName,
                    mimeType = mimeType,
                    size = size,
                    flags = flags,
                    lastMod = lastMod,
                    mediaUri = docid.mediaUri
                    //summary,
            ),
            resumeInfo = resume
    )
}

fun DocMusicTrackRef.contentValues(): ContentValues {
    val values = ContentValues()
    values.put("authority", id.treeUri.authority)
    values.put("tree_uri", id.treeUri.toString())
    values.put("parent_id", id.parentId)
    values.put("document_id", id.documentId)
    values.put("_display_name", meta.originalTitle.elseIfBlank(meta.title))
    values.put("mime_type", meta.mimeType)
    values.put("last_modified", meta.lastMod)
    values.put("flags", meta.flags)
    values.put("_size", meta.size)
    values.put("date_added", System.currentTimeMillis())
    values.put("hidden", 0)
    return values
}

val docMusicTrackProjection = arrayOf(
        //upnp_music_track columns
        "t.tree_uri", "t.parent_id", "t.document_id", //2
        "t._display_name", "t.mime_type", "t.last_modified", "t.flags", //6
        "t._size" //7
)

fun Cursor.toDocMusicTrackRef(helper: ApiHelper): DocMusicTrackRef {
    val tree_uri = Uri.parse(getString(0))
    val parent_id = getString(1)
    val document_id = getString(2)
    val title = getString(3)
    val mime = getString(4)
    val last_mod = getLong(5)
    val flags = getLong(6)
    val size = getLong(7)
    val id = DocMusicTrackId(
            treeUri = tree_uri,
            parentId = parent_id,
            documentId = document_id
    )
    return DocMusicTrackRef(
            id = id,
            meta = DocMusicTrackMeta(
                    title = title,
                    mimeType = mime,
                    lastMod = last_mod,
                    flags = flags,
                    size = size,
                    mediaUri = id.mediaUri
            )
    )
}

fun StorageDeviceRef.contentValues(): ContentValues {
    val values = ContentValues()
    values.put("uuid", id.uuid)
    values.put("path", id.path)
    values.put("is_primary", if (id.isPrimary) 1 else 0)
    values.put("_display_name", meta.title)
    values.put("hidden", 0)
    return values
}

val storageDeviceProjection = arrayOf(
        "uuid", "path", "is_primary", "_display_name"
)

fun Cursor.toStorageDevice(): StorageDeviceRef {
    val uuid = getString(0)
    val path = getString(1)
    val primary = getLong(2) == 1L
    val title = getString(3)
    return StorageDeviceRef(
            id = StorageDeviceId(
                    uuid = uuid,
                    path = path,
                    isPrimary = primary
            ),
            meta = StorageDeviceMeta(
                    title = title
            )
    )
}

fun StorageFolderRef.contentValues(): ContentValues {
    val values = ContentValues(10)
    values.put("path", id.path)
    values.put("parent_path", id.parent)
    values.put("device_uuid", id.uuid)
    values.put("_display_name", meta.title)
    values.put("hidden", 0)
    return values
}

val storageFolderProjection = arrayOf(
        "f.path", "device_uuid", "parent_path", "f._display_name"
)

fun Cursor.toStorageFolder(): StorageFolderRef {
    val path = getString(0)
    val uuid = getString(1)
    val parent = getString(2)
    val title = getString(3)
    return StorageFolderRef(
            id = StorageFolderId(
                    path = path,
                    uuid = uuid,
                    parent = parent
            ),
            meta = StorageFolderMeta(
                    title = title
            )
    )
}

fun StorageVideoRef.contentValues(): ContentValues {
    val values = ContentValues()
    values.put("path", id.path)
    values.put("parent_path", id.parent)
    values.put("device_uuid", id.uuid)
    values.put("_display_name", meta.originalTitle.elseIfBlank(meta.title))
    values.put("mime_type", meta.mimeType)
    values.put("last_modified", meta.lastMod)
    values.put("_size", meta.size)
    values.put("date_added", System.currentTimeMillis())
    values.put("hidden", 0)
    return values
}

val storageVideoProjection = arrayOf(
        //video columns
        "v.path", "v.parent_path", "v.device_uuid", "d.hidden", //3
        "v._display_name", "v.mime_type", "v._size", "v._size", "v.last_modified", //8
        //episode columns
        "e._id", "e._display_name", //10
        "e.episode_number", "e.season_number", //12
        //series columns
        "s._id", "s._display_name", //14
        "s.poster", "s.backdrop", //16
        //movie columns
        "m._id", "m._display_name",  //18
        "m.poster_path", "m.backdrop_path", //20
        //more episode columns
        "e.poster", "e.backdrop", //22
        //completion info
        "p.last_position", "p.last_completion", "p.last_played" //25
)

fun Cursor.toStorageVideo(mApiHelper: ApiHelper): StorageVideoRef {
    val path = getString(0)
    val parent = getString(1)
    val uuid = getString(2)
    var title = getString(4)
    val originalTitle = title
    val mimeType = getString(5)
    val mediaUri = Uri.parse(getString(0))
    val size = getLong(6)
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
    var resume: VideoResumeInfo? = null
    if (!isNull(23) && !isNull(24) && !isNull(25)) {
        resume = VideoResumeInfo(lastPosition = getLong(23),
                lastCompletion = getInt(24), lastPlayed = getLong(25))
    }
    return StorageVideoRef(
            id = StorageVideoId(
                    path = path,
                    parent = parent,
                    uuid = uuid
            ),
            tvEpisodeId = episodeId,
            movieId = movieId,
            meta = StorageVideoMeta(
                    title = title,
                    originalTitle = originalTitle,
                    subtitle = subtitle,
                    artworkUri = artworkUri,
                    backdropUri = backdropUri,
                    mimeType = mimeType,
                    mediaUri = mediaUri,
                    size = size
            ),
            resumeInfo = resume
    )
}

fun StorageMusicTrackRef.contentValues(): ContentValues {
    val values = ContentValues()
    values.put("path", id.path)
    values.put("parent_path", id.parent)
    values.put("device_uuid", id.uuid)
    values.put("_display_name", meta.originalTitle.elseIfBlank(meta.title))
    values.put("mime_type", meta.mimeType)
    values.put("last_modified", meta.lastMod)
    values.put("_size", meta.size)
    values.put("date_added", System.currentTimeMillis())
    values.put("hidden", 0)
    return values
}

val storageMusicTrackProjection = arrayOf(
        //upnp_music_track columns
        "t.path", "t.parent_path", "t.device_uuid", //2
        "t._display_name", "t.mime_type", "t.last_modified", "t._size" //6
)

fun Cursor.toStorageMusicTrack(helper: ApiHelper): StorageMusicTrackRef {
    val path = getString(0)
    val parent = getString(1)
    val uuid = getString(2)
    val title = getString(3)
    val mime = getString(4)
    val last_mod = getLong(5)
    val size = getLong(6)
    val uri = Uri.parse(path)
    return StorageMusicTrackRef(
            id = StorageMusicTrackId(
                    uuid = uuid,
                    path = path,
                    parent = parent
            ),
            meta = StorageMusicTrackMeta(
                    title = title,
                    mimeType = mime,
                    lastMod = last_mod,
                    size = size,
                    mediaUri = uri
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

fun positionContentVals(position: Long, duration: Long): ContentValues {
    val values = ContentValues()
    values.put("last_played", System.currentTimeMillis())
    values.put("last_position", position)
    values.put("last_completion", calculateCompletion(position, duration))
    return values
}

fun makeTvSubtitle(seriesName: String, seasonNumber: Int, episodeNumber: Int): String =
        "$seriesName - S${seasonNumber.zeroPad(2)}E${episodeNumber.zeroPad(2)}"

fun Int.zeroPad(len: Int): String =
        this.toString().padStart(len, '0')

/**
 * scale is 0-1000
 * uses permyriad calculation (no floating point)
 */
fun calculateCompletion(current: Long, duration: Long): Int {
    return ((1000*current + duration/2)/duration).toInt()
}

fun contentValues(key: String, value: String): ContentValues {
    val values = ContentValues()
    values.put(key, value)
    return values
}

fun contentValues(key: String, value: Int): ContentValues {
    val values = ContentValues()
    values.put(key, value)
    return values
}

fun contentValues(key: String, value: Long): ContentValues {
    val values = ContentValues()
    values.put(key, value)
    return values
}
