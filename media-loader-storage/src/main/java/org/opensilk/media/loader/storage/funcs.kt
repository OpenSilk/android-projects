package org.opensilk.media.loader.storage

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.support.v4.content.ContextCompat
import android.webkit.MimeTypeMap
import org.opensilk.media.*
import java.io.File

/**
 * Created by drew on 8/24/17.
 */
internal fun Context.canReadPrimaryStorage() = ContextCompat.checkSelfPermission(this,
        Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED

internal fun Context.hasAccessActivity(): Boolean {
    val activities = packageManager.queryIntentActivities(Intent(
            "android.os.storage.action.OPEN_EXTERNAL_DIRECTORY"), PackageManager.MATCH_DEFAULT_ONLY)
    return activities != null && activities.size > 0
}

internal fun File.mimeType() = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "application/unknown"

internal fun File.isVideoFile() = mimeType().startsWith("video")

internal fun File.toDirectoryRef(uuid: String): StorageFolderRef {
    return StorageFolderRef(
            id = StorageFolderId(
                    path = path,
                    uuid = uuid,
                    parent = parent
            ),
            meta = StorageFolderMeta(
                    title = name
            )
    )
}

internal fun File.toVideoRef(uuid: String): StorageVideoRef {
    return StorageVideoRef(
            id = StorageVideoId(
                    path = path,
                    uuid = uuid,
                    parent = parent
            ),
            meta = StorageVideoMeta(
                    title = name,
                    size = length(),
                    lastMod = lastModified(),
                    mimeType = mimeType(),
                    mediaUri = Uri.parse(path)
            )
    )
}