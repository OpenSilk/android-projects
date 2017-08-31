package org.opensilk.media.playback

import android.media.session.PlaybackState
import android.media.session.PlaybackState.*

/**
 * Created by drew on 6/29/17.
 */
fun PlaybackState.hasAction(action: Long): Boolean =
        (actions and action) == action

fun Long.toPlaybackActionString(): String = when(this) {
    ACTION_PLAY_FROM_MEDIA_ID -> "ACTION_PLAY_FROM_MEDIA_ID"
    ACTION_PAUSE -> "ACTION_PAUSE"
    ACTION_PLAY -> "ACTION_PLAY"
    ACTION_SKIP_TO_NEXT -> "ACTION_SKIP_TO_NEXT"
    ACTION_SKIP_TO_PREVIOUS -> "ACTION_SKIP_TO_PREVIOUS"
    ACTION_SKIP_TO_QUEUE_ITEM -> "ACTION_SKIP_TO_QUEUE_ITEM"
    ACTION_SEEK_TO -> "ACTION_SEEK_TO"
    ACTION_FAST_FORWARD -> "ACTION_FAST_FORWARD"
    ACTION_PLAY_FROM_SEARCH -> "ACTION_PLAY_FROM_SEARCH"
    ACTION_REWIND -> "ACTION_REWIND"
    ACTION_STOP -> "ACTION_STOP"
    ACTION_SET_RATING -> "ACTION_SET_RATING"
    else -> "UNKNOWN($this)"
}

fun PlaybackState._stringify(): String = when (state) {
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
