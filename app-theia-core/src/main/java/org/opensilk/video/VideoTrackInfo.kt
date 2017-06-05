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
        val width: Int = 0,
        val height: Int = 0,
        val bitrate: Int = 0,
        val frameRate: Int = 0,
        val frameRateDen: Int = 0
) {
    override fun toString(): String {
        return String.format(Locale.US, "%s %dx%d %s %.02ffps",
                codec,
                width,
                height,
                humanReadableBitrate(bitrate.toLong()),
                frameRate.toFloat() / frameRateDen)
    }

    companion object {
        @BindingAdapter("android:text") @JvmStatic
        fun bindInfoToTextView(textView: TextView, videoTrackInfo: VideoTrackInfo?) {
            textView.visibility = if (videoTrackInfo != null) View.VISIBLE else View.INVISIBLE
            textView.text = videoTrackInfo?.toString() ?: ""
        }
    }
}