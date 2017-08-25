package org.opensilk.media.database

import org.opensilk.media.*

/**
 * Created by drew on 8/11/17.
 */
sealed class DatabaseChange
class UpnpUpdateIdChange(val updateId: Long): DatabaseChange()
class UpnpDeviceChange: DatabaseChange()
class UpnpFolderChange(val folderId: UpnpFolderId): DatabaseChange()
class UpnpVideoChange(val videoId: UpnpVideoId): DatabaseChange()
class VideoDocumentChange(val documentId: DocumentId): DatabaseChange()
class StorageVideoChange(val videoId: StorageVideoId): DatabaseChange()
