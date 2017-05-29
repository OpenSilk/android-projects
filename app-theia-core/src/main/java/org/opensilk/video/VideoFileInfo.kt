package org.opensilk.video

import android.net.Uri

/**
 * Created by drew on 5/28/17.
 */
data class VideoFileInfo(
        val uri: Uri,
        val title: String,
        val sizeBytes: Long = -1,
        val durationMilli: Long = -1,
        val firstAudioTrack: AudioTrackInfo? = null,
        val secondAudioTrack: AudioTrackInfo? = null,
        val firstVideoTrack: VideoTrackInfo? = null
) {
    val sizeString: String
        get() = org.opensilk.video.humanReadableSize(sizeBytes)

    val durationString: String
        get() = org.opensilk.video.humanReadableDuration(durationMilli)
}