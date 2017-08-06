package org.opensilk.video

import android.media.MediaDescription
import org.opensilk.media._getMediaMeta

/**
 * Created by drew on 5/28/17.
 */
data class VideoProgressInfo(
        val position: Long = 0,
        val duration: Long = 0
) {
    val completion: Float by lazy {
        if (position > 0 && duration > 0) {
            return@lazy (position.toFloat()) / (duration.toFloat())
        }
        return@lazy 0f
    }

    val progress: Int by lazy {
        return@lazy (completion * 100).toInt()
    }
}
