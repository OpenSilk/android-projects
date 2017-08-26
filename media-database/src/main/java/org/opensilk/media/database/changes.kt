package org.opensilk.media.database

import org.opensilk.media.*

/**
 * Represents database changes, used in lieu of contentObservers
 */
sealed class DatabaseChange

/**
 * Represents a change in media devices
 */
open class DeviceChange: DatabaseChange()

/**
 * Represents a change in a video ref
 */
open class VideoChange(open val videoId: VideoId): DatabaseChange()

class UpnpUpdateIdChange(val updateId: Long): DatabaseChange()
class UpnpDeviceChange: DeviceChange()
class UpnpFolderChange(val folderId: UpnpFolderId): DatabaseChange()
class UpnpVideoChange(override val videoId: UpnpVideoId): VideoChange(videoId)
class DocVideoChange(override val videoId: DocVideoId): VideoChange(videoId)
class StorageDeviceChange(): DeviceChange()
class StorageVideoChange(override val videoId: StorageVideoId): VideoChange(videoId)
