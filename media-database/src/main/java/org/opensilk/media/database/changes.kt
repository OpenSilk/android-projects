package org.opensilk.media.database

import org.opensilk.media.*

/**
 * Represents database changes, used in lieu of contentObservers
 */
sealed class DatabaseChange

class UpnpUpdateIdChange(val updateId: Long): DatabaseChange()

/**
 * Represents a change in media devices
 */
open class DeviceChange: DatabaseChange()
class UpnpDeviceChange: DeviceChange()
class StorageDeviceChange: DeviceChange()

/**
 * Represents a change in a video ref
 */
open class VideoChange(open val videoId: VideoId): DatabaseChange()
class UpnpVideoChange(override val videoId: UpnpVideoId): VideoChange(videoId)
class DocVideoChange(override val videoId: DocVideoId): VideoChange(videoId)
class StorageVideoChange(override val videoId: StorageVideoId): VideoChange(videoId)

/**
 * Represent a change in a folder ref
 */
open class FolderChange(open val folderId: FolderId): DatabaseChange()
class UpnpFolderChange(override val folderId: UpnpFolderId): FolderChange(folderId)
class DocDirectoryChange(override val folderId: DocDirectoryId): FolderChange(folderId)
class StorageFolderChange(override val folderId: StorageFolderId): FolderChange(folderId)
