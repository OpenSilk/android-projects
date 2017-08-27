package org.opensilk.video

import io.reactivex.Observable
import io.reactivex.Single
import org.opensilk.media.*
import org.opensilk.media.database.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * The loader for the folder activity. This pulls folders and videos from
 * the database, inserted by the [FolderPrefetchLoader]. We initiate reloads whenever any
 * of our children are changed, allowing the ui to refresh the completion indicator
 * without listening for changes itself.
 */
class FoldersLoader
@Inject constructor(
        private val mDatabaseClient: MediaDAO
) {

    /**
     * Retrieve direct children. This observable never completes
     */
    fun directChildren(deviceId: MediaDeviceId): Observable<out List<MediaRef>> {

        val items = when (deviceId) {
            is UpnpDeviceId -> upnpDisk(deviceId)
            is StorageDeviceId -> storageDisk(deviceId)
            else -> TODO()
        }
                //during lookup we can be flooded
        return changesForDevice(deviceId)
                .mergeWith(changesForDeviceChildren(deviceId))
                .sample(3, TimeUnit.SECONDS)
                .startWith(Change.SELF)
                .switchMapSingle { _ ->
                    items.subscribeOn(AppSchedulers.diskIo)
                }
    }

    /**
     * Retrieve direct children. This observable never completes
     */
    fun directChildren(folderId: FolderId): Observable<out List<MediaRef>> {

        val items = when (folderId) {
            is UpnpFolderId -> upnpDisk(folderId)
            is DocDirectoryId -> documentDisk(folderId)
            is StorageFolderId -> storageDisk(folderId)
            else -> TODO()
        }

        return changesForFolder(folderId)
                .mergeWith(changesForFolderChildren(folderId))
                .sample(3, TimeUnit.SECONDS)
                .startWith(Change.SELF)
                .switchMapSingle { _ ->
                    items.subscribeOn(AppSchedulers.diskIo)
                }
    }

    private enum class Change {
        SELF,
        CHILDREN
    }

    private fun changesForFolder(folderId: FolderId): Observable<Change> {
        return mDatabaseClient.changesObservable
                .filter { it is FolderChange && it.folderId == folderId }
                .map { Change.SELF }
    }

    private fun changesForFolderChildren(folderId: FolderId): Observable<Change> {
        return mDatabaseClient.changesObservable
                .filter { when (folderId) {
                    is UpnpFolderId -> when (it) {
                        is UpnpFolderChange -> it.folderId.parentId == folderId.containerId
                        is UpnpVideoChange -> it.videoId.parentId == folderId.containerId
                        else -> false
                    }
                    is DocDirectoryId -> when (it) {
                        is DocDirectoryChange -> it.folderId.parentId == folderId.documentId
                        is DocVideoChange -> it.videoId.parentId == folderId.documentId
                        else -> false
                    }
                    is StorageFolderId -> when (it) {
                        is StorageFolderChange -> it.folderId.parent == folderId.path
                        is StorageVideoChange -> it.videoId.parent == folderId.path
                        else -> false
                    }
                    else -> TODO()
                } }.map { Change.CHILDREN }
    }

    private fun changesForDevice(deviceId: MediaDeviceId): Observable<Change> {
        return mDatabaseClient.changesObservable
                .filter { when (deviceId) {
                    is UpnpDeviceId -> it is UpnpDeviceChange
                    is StorageDeviceId -> it is StorageDeviceChange
                    else -> false
                } }.map { Change.SELF }
    }

    private fun changesForDeviceChildren(deviceId: MediaDeviceId): Observable<Change> {
        return mDatabaseClient.changesObservable
                .filter { when (deviceId) {
                    is UpnpDeviceId -> when (it) {
                        is UpnpFolderChange -> it.folderId.parentId == deviceId.containerId
                        is UpnpVideoChange -> it.videoId.parentId == deviceId.containerId
                        else -> false
                    }
                    is StorageDeviceId -> when (it) {
                        is StorageFolderChange -> it.folderId.parent == deviceId.path
                        is StorageVideoChange -> it.videoId.parent == deviceId.path
                        else -> false
                    }
                    else -> TODO()
                } }.map { Change.CHILDREN }
    }

    private fun upnpDisk(folderId: UpnpContainerId): Single<List<UpnpRef>> {
        return Observable.concat(
                mDatabaseClient.getUpnpFoldersUnder(folderId),
                mDatabaseClient.getUpnpVideosUnder(folderId)
        ).toList()
    }

    private fun storageDisk(containerId: StorageContainerId): Single<List<StorageRef>> {
        return Observable.concat(
                mDatabaseClient.getStorageFoldersUnder(containerId),
                mDatabaseClient.getStorageVideosUnder(containerId)
        ).toList()
    }

    private fun documentDisk(documentId: DocDirectoryId): Single<List<DocumentRef>> {
        return Observable.concat<DocumentRef>(
                mDatabaseClient.getDocDirectoryUnder(documentId),
                mDatabaseClient.getDocVideosUnder(documentId)
        ).toList()
    }

}