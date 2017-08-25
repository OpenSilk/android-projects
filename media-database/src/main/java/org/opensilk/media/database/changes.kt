package org.opensilk.media.database

import org.opensilk.media.*

/**
 * Created by drew on 8/11/17.
 */
sealed class DatabaseChange
open class VideoChange(open val videoId: VideoId): DatabaseChange()
class UpnpUpdateIdChange(val updateId: Long): DatabaseChange()
class UpnpDeviceChange: DatabaseChange()
class UpnpFolderChange(val folderId: UpnpFolderId): DatabaseChange()
class UpnpVideoChange(override val videoId: UpnpVideoId): VideoChange(videoId)
class DocVideoChange(override val videoId: DocVideoId): VideoChange(videoId)
class StorageVideoChange(override val videoId: StorageVideoId): VideoChange(videoId)
