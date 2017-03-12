package org.opensilk.media.playback

import android.media.MediaDescription
import android.media.browse.MediaBrowser
import android.media.session.MediaSession.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * Created by drew on 2/24/17.
 */
class PlaybackQueue {

    private val mIdGen = AtomicLong(1)

    companion object {
        val EMPTY_DESCRIPTION: MediaDescription = MediaDescription.Builder().build()
        val EMPTY_ITEM: QueueItem = QueueItem(EMPTY_DESCRIPTION, 0)
    }

    fun goToPrevious(): QueueItem {
        return EMPTY_ITEM
    }

    fun goToNext(): QueueItem {
        return EMPTY_ITEM
    }
}