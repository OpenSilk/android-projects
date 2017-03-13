package org.opensilk.music.data

import android.media.browse.MediaBrowser
import org.opensilk.common.misc.AlphanumComparator
import org.opensilk.media._getMediaMeta
import java.util.*

/**
 * Created by drew on 8/4/16.
 */

object DefaultCompare: Comparator<MediaBrowser.MediaItem> {
    override fun compare(lhs: MediaBrowser.MediaItem?, rhs: MediaBrowser.MediaItem?): Int {
        return 0
    }
}

object AscendingCompare: Comparator<MediaBrowser.MediaItem> {
    override fun compare(lhs: MediaBrowser.MediaItem?, rhs: MediaBrowser.MediaItem?): Int {
        lhs!!; rhs!!
        val lmeta = lhs._getMediaMeta()
        val rmeta = rhs._getMediaMeta()
        //directories first
        if (lmeta.isDirectory && !rmeta.isDirectory) {
            return -1
        } else if (!lmeta.isDirectory && rmeta.isDirectory) {
            return 1
        } else {
            return AlphanumComparator.compare(lmeta.displayName, rmeta.displayName)
        }
    }
}

object DecendingCompare: Comparator<MediaBrowser.MediaItem> {
    override fun compare(lhs: MediaBrowser.MediaItem?, rhs: MediaBrowser.MediaItem?): Int {
        //swap them
        return AscendingCompare.compare(rhs, lhs)
    }
}