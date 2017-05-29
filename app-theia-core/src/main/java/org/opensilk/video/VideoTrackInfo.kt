package org.opensilk.video

import android.databinding.BindingAdapter
import android.view.View
import android.widget.TextView
import java.util.*

/**
 * Created by drew on 5/28/17.
 */
data class VideoTrackInfo(
        val codec: String = "",
        val width: Int = -1,
        val height: Int = -1,
        val bitrate: Int = -1,
        val frameRate: Int = -1,
        val frameRateDen: Int = -1
) {
    override fun toString(): String {
        return String.format(Locale.US, "%s %dx%d %s %.02ffps",
                codec,
                width,
                height,
                humanReadableBitrate(bitrate.toLong()),
                frameRate.toFloat() / frameRateDen)
    }

    @BindingAdapter("android:text")
    fun bindInfoToTextView(textView: TextView, videoTrackInfo: VideoTrackInfo?) {
        textView.visibility = if (videoTrackInfo != null) View.VISIBLE else View.INVISIBLE
        textView.text = videoTrackInfo?.toString() ?: ""
    }
}