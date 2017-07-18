package org.opensilk.video

import android.media.MediaDescription

/**
 * Created by drew on 5/28/17.
 */
data class VideoDescInfo(
        val title: String = "",
        val subtitle: String = "",
        val overview: String = ""
)

fun MediaDescription.videoDescInfo(): VideoDescInfo {
    return VideoDescInfo(title.toString(), subtitle?.toString() ?: "", description?.toString() ?: "")
}