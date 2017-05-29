package org.opensilk.video

import android.databinding.BindingAdapter
import android.view.View
import android.widget.TextView
import java.util.*

/**
 * Created by drew on 5/28/17.
 */
data class AudioTrackInfo(
        val codec: String = "",
        val bitrate: Int = -1,
        val rate: Int = -1,
        val channels: Int = -1
) {
    override fun toString(): String {
        return String.format(Locale.US, "%s %s %dHz %d channels",
                codec,
                humanReadableBitrate(bitrate.toLong()),
                rate,
                channels)
    }

    @BindingAdapter("android:text")
    fun bindInfoToTextView(textView: TextView, audioTrackInfo: AudioTrackInfo?) {
        textView.visibility = if (audioTrackInfo != null) View.VISIBLE else View.INVISIBLE
        textView.text = audioTrackInfo?.toString() ?: ""
    }
}
