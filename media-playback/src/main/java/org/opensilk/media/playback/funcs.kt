package org.opensilk.media.playback

import android.media.session.PlaybackState
import android.media.session.PlaybackState.*

/**
 * Created by drew on 6/29/17.
 */
fun PlaybackState.hasAction(action: Long): Boolean {
    return (actions and action) == action
}

fun PlaybackState._stringify(): String {
    return when (state) {
        STATE_NONE -> "STATE_NONE"
        STATE_STOPPED -> "STATE_STOPPED"
        STATE_PAUSED -> "STATE_PAUSED"
        STATE_PLAYING -> "STATE_PLAYING"
        STATE_FAST_FORWARDING -> "STATE_FAST_FORWARDING"
        STATE_REWINDING -> "STATE_REWINDING"
        STATE_BUFFERING -> "STATE_BUFFERING"
        STATE_ERROR -> "STATE_ERROR"
        STATE_CONNECTING -> "STATE_CONNECTING"
        STATE_SKIPPING_TO_PREVIOUS -> "STATE_SKIPPING_TO_PREVIOUS"
        STATE_SKIPPING_TO_NEXT -> "STATE_SKIPPING_TO_NEXT"
        STATE_SKIPPING_TO_QUEUE_ITEM -> "STATE_SKIPPING_TO_QUEUE_ITEM"
        else -> "UNKNOWN #" + state
    }
}

fun PlaybackState._newBuilder(): PlaybackState.Builder {
    val bob = PlaybackState.Builder()
            .setState(state, position, playbackSpeed, lastPositionUpdateTime)
            .setActions(actions)
            .setActiveQueueItemId(activeQueueItemId)
            .setBufferedPosition(bufferedPosition)
            .setErrorMessage(errorMessage)
            .setExtras(extras)
    for (action in customActions) {
        bob.addCustomAction(action)
    }
    return bob
}
