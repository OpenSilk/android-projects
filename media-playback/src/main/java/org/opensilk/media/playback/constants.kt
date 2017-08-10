package org.opensilk.media.playback

/**
 * Created by drew on 8/10/17.
 */
const val ACTION_SET_REPEAT = "org.opensilk.media.ACTION_SET_REPEAT"
const val KEY_REPEAT = "org.opensilk.media.KEY_REPEAT"
const val VAL_REPEAT_OFF = 1
const val VAL_REPEAT_ON = 2 //default

// The volume we set the media player to when we lose audio focus, but are
// allowed to reduce the volume instead of stopping playback.
internal const val VOLUME_DUCK = 0.2f
// The volume we set the media player when we have audio focus.
internal const val VOLUME_NORMAL = 1.0f