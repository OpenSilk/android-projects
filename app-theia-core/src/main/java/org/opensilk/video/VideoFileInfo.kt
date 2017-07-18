package org.opensilk.video

/**
 * Created by drew on 5/28/17.
 */
data class VideoFileInfo(
        val title: String = "",
        val sizeBytes: Long = 0,
        val durationMilli: Long = 0,
        val firstAudioTrack: AudioTrackInfo? = null,
        val secondAudioTrack: AudioTrackInfo? = null,
        val firstVideoTrack: VideoTrackInfo? = null
) {
    val sizeString: String
        get() = if (sizeBytes > 0) humanReadableSize(sizeBytes) else ""

    val durationString: String
        get() = if (durationMilli > 0) humanReadableDuration(durationMilli) else ""
}