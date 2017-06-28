package org.opensilk.media.playback

import android.os.Bundle

/**
 * Created by drew on 6/8/17.
 */
class PlaybackExtras {
    private val bundle: Bundle
    public constructor(): this(Bundle())
    internal constructor(bundle: Bundle) {
        this.bundle = bundle
    }

    var resume: Boolean
        set(value) = bundle.putBoolean("resume", value)
        get() = bundle.getBoolean("resume", false)

    fun bundle() : Bundle {
        return Bundle(bundle)
    }
}

fun Bundle?._playbackExtras(): PlaybackExtras {
    return if (this != null) PlaybackExtras(this) else PlaybackExtras()
}
