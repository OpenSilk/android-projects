package org.opensilk.music.ui.recycler

import android.media.MediaDescription
import android.media.browse.MediaBrowser
import android.net.Uri
import org.opensilk.music.R
import org.opensilk.media.MediaMeta
import org.opensilk.media._getMediaMeta

/**
 * Created by drew on 7/1/16.
 */
open class MediaTile
constructor(
        private var mMediaItem: MediaBrowser.MediaItem
) {

    val title: String
        get() = mMediaItem.description.title?.toString() ?: meta.displayName

    val subtitle: String
        get() = mMediaItem.description.subtitle?.toString() ?: ""

    val item: MediaBrowser.MediaItem
        get() = mMediaItem

    val description: MediaDescription
        get() = mMediaItem.description

    val meta: MediaMeta by lazy {
        mMediaItem._getMediaMeta()
    }

    val iconUri: Uri?
        get() = mMediaItem.description.iconUri

    val letterTileText: String
        get() {
            var letterText = title
            if (meta.isAudio) {
                if (meta.trackNumber > 0) {
                    letterText = meta.trackNumber.toString()
                } else if (meta.displayName != "") {
                    letterText = meta.displayName
                }
            }
            return letterText
        }

    var tileLayout: Int = -1
}
