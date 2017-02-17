package org.opensilk.music.playback

import android.media.session.MediaSession
import java.util.concurrent.atomic.AtomicInteger

/**
 * Created by drew on 8/26/16.
 */
class PlaybackQueue {

    private val currentItemId: AtomicInteger = AtomicInteger(0)

    fun nextItemId(): Int {
        return currentItemId.incrementAndGet()
    }





}